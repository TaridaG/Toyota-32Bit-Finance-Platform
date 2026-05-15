package com.company.marketdataservice.fx.evds;

import com.company.marketdataservice.config.MarketEvdsProperties;
import com.company.marketdataservice.historical.HistoricalFxPoint;
import com.company.marketdataservice.historical.HistoricalFxProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component
public class EvdsHistoricalFxProvider implements HistoricalFxProvider {

    private static final Logger log = LoggerFactory.getLogger(EvdsHistoricalFxProvider.class);
    private static final DateTimeFormatter EVDS_DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu");
    private static final DateTimeFormatter TCMB_YEAR_MONTH = DateTimeFormatter.ofPattern("uuuuMM");
    private static final DateTimeFormatter TCMB_DAY_FILE = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final MarketEvdsProperties evdsProperties;
    private final ObjectMapper objectMapper;
    private final WebClient fxWebClient;

    public EvdsHistoricalFxProvider(
            MarketEvdsProperties evdsProperties,
            ObjectMapper objectMapper,
            @Qualifier("fxWebClient") WebClient fxWebClient
    ) {
        this.evdsProperties = evdsProperties;
        this.objectMapper = objectMapper;
        this.fxWebClient = fxWebClient;
    }

    @Override
    public List<HistoricalFxPoint> fetchRange(String symbol, LocalDate startDate, LocalDate endDate) {
        if (symbol == null || symbol.isBlank() || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return List.of();
        }
        if (evdsProperties.getApiKey() == null || evdsProperties.getApiKey().isBlank()) {
            log.warn("EVDS historical FX skipped: apiKey is missing");
            return List.of();
        }

        String canonical = symbol.trim().toUpperCase(Locale.ROOT);
        Pair pair = resolvePair(canonical);
        if (pair == null) {
            return List.of();
        }

        // Prefer deterministic TCMB archive files for stable historical backfill coverage.
        List<HistoricalFxPoint> archivePoints = fetchFromTcmbArchive(canonical, pair, startDate, endDate);
        if (!archivePoints.isEmpty()) {
            return archivePoints;
        }

        String series = "TP.DK." + pair.baseCurrency + ".S.YTL";
        String uri = buildUri(series, startDate, endDate);
        if (uri == null) {
            return List.of();
        }

        try {
            String body = fxWebClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (body == null || body.isBlank()) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                return List.of();
            }

            List<HistoricalFxPoint> out = new ArrayList<>();
            for (JsonNode item : items) {
                LocalDate d = parseDate(item.path("Tarih").asText(null));
                if (d == null) {
                    continue;
                }

                BigDecimal mid = parseDecimal(
                        firstNonEmpty(
                                item,
                                "TP_DK_" + pair.baseCurrency + "_S_YTL",
                                "TP_DK_" + pair.baseCurrency + "_A_YTL",
                                pair.baseCurrency
                        )
                );
                if (mid == null) {
                    continue;
                }
                if ("JPYTRY".equals(canonical)) {
                    mid = mid.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                }

                BigDecimal spread = mid.multiply(new BigDecimal("0.001"));
                BigDecimal bid = mid.subtract(spread);
                BigDecimal ask = mid.add(spread);
                Instant observedAt = d.atStartOfDay().toInstant(ZoneOffset.UTC);

                out.add(new HistoricalFxPoint(
                        canonical,
                        null,
                        pair.baseCurrency,
                        pair.quoteCurrency,
                        bid,
                        ask,
                        mid,
                        observedAt,
                        "EVDS"
                ));
            }

            out.sort(Comparator.comparing(HistoricalFxPoint::occurredAt));
            if (!out.isEmpty()) {
                return out;
            }
            return fetchFromTcmbArchive(canonical, pair, startDate, endDate);
        } catch (Exception ex) {
            log.warn(
                    "EVDS_HISTORICAL_FX_FETCH_FAILED symbol={} start={} end={} reason={}",
                    canonical,
                    startDate,
                    endDate,
                    ex.getMessage()
            );
            return fetchFromTcmbArchive(canonical, pair, startDate, endDate);
        }
    }

    private String buildUri(String series, LocalDate startDate, LocalDate endDate) {
        String base = evdsProperties.getBaseUrl();
        if (base == null || base.isBlank()) {
            log.warn("EVDS historical FX skipped: baseUrl is missing");
            return null;
        }
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return normalized
                + "/series="
                + series
                + "&startDate=" + EVDS_DATE.format(startDate)
                + "&endDate=" + EVDS_DATE.format(endDate)
                + "&type=json"
                + "&key=" + evdsProperties.getApiKey();
    }

    private static Pair resolvePair(String canonical) {
        if ("USDTRY".equals(canonical)) {
            return new Pair("USD", "TRY");
        }
        if ("EURTRY".equals(canonical)) {
            return new Pair("EUR", "TRY");
        }
        if ("GBPTRY".equals(canonical)) {
            return new Pair("GBP", "TRY");
        }
        if ("JPYTRY".equals(canonical)) {
            return new Pair("JPY", "TRY");
        }
        if ("AEDTRY".equals(canonical)) {
            return new Pair("AED", "TRY");
        }
        if ("XAUTRY".equals(canonical)) {
            return new Pair("XAU", "TRY");
        }
        if ("XAGTRY".equals(canonical)) {
            return new Pair("XAG", "TRY");
        }
        if ("XPTTRY".equals(canonical)) {
            return new Pair("XPT", "TRY");
        }
        if ("XPDTRY".equals(canonical)) {
            return new Pair("XPD", "TRY");
        }
        if ("XCUTRY".equals(canonical)) {
            return new Pair("XCU", "TRY");
        }
        return null;
    }

    private List<HistoricalFxPoint> fetchFromTcmbArchive(String canonical, Pair pair, LocalDate startDate, LocalDate endDate) {
        List<HistoricalFxPoint> out = new ArrayList<>();
        LocalDate day = startDate;
        while (!day.isAfter(endDate)) {
            String url = "https://www.tcmb.gov.tr/kurlar/"
                    + TCMB_YEAR_MONTH.format(day)
                    + "/"
                    + TCMB_DAY_FILE.format(day)
                    + ".xml";
            try {
                String xml = fxWebClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                if (xml != null && !xml.isBlank()) {
                    HistoricalFxPoint p = parseTcmbPoint(xml, canonical, pair, day);
                    if (p != null) {
                        out.add(p);
                    }
                }
            } catch (Exception ignore) {
                // Skip missing/non-trading days or unavailable files.
            }
            day = day.plusDays(1);
        }
        out.sort(Comparator.comparing(HistoricalFxPoint::occurredAt));
        if (!out.isEmpty()) {
            log.info("FX_HISTORY_FALLBACK_TCMB_USED symbol={} points={}", canonical, out.size());
        }
        return out;
    }

    private static HistoricalFxPoint parseTcmbPoint(String xml, String canonical, Pair pair, LocalDate day) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setExpandEntityReferences(false);
            try {
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            } catch (Exception ignored) {
            }
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            NodeList currencies = doc.getElementsByTagName("Currency");
            for (int i = 0; i < currencies.getLength(); i++) {
                if (!(currencies.item(i) instanceof Element el)) {
                    continue;
                }
                String code = textOrNull(el.getAttribute("Kod"));
                if (code == null) {
                    code = textOrNull(el.getAttribute("CurrencyCode"));
                }
                if (code == null || !pair.baseCurrency.equalsIgnoreCase(code.trim())) {
                    continue;
                }
                BigDecimal bid = parseDecimal(firstNonEmptyXml(el, "ForexBuying", "BanknoteBuying"));
                BigDecimal ask = parseDecimal(firstNonEmptyXml(el, "ForexSelling", "BanknoteSelling"));
                if (bid == null || ask == null) {
                    continue;
                }
                BigDecimal mid = bid.add(ask).divide(new BigDecimal("2"), 6, java.math.RoundingMode.HALF_UP);
                Instant observedAt = day.atStartOfDay().toInstant(ZoneOffset.UTC);
                // TCMB lists JPY as TRY per 100 JPY; our canonical hub uses TRY per 1 JPY (same as other crosses).
                if ("JPYTRY".equals(canonical)) {
                    bid = bid.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                    ask = ask.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                    mid = mid.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP);
                }
                return new HistoricalFxPoint(
                        canonical,
                        null,
                        pair.baseCurrency,
                        pair.quoteCurrency,
                        bid,
                        ask,
                        mid,
                        observedAt,
                        "TCMB_ARCHIVE"
                );
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw, EVDS_DATE);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String firstNonEmpty(JsonNode node, String... names) {
        for (String n : names) {
            JsonNode v = node.get(n);
            if (v != null && !v.isNull()) {
                String s = v.asText();
                if (s != null && !s.isBlank()) {
                    return s;
                }
            }
        }
        return null;
    }

    private static String firstNonEmptyXml(Element el, String... tags) {
        for (String tag : tags) {
            NodeList nodes = el.getElementsByTagName(tag);
            if (nodes.getLength() > 0 && nodes.item(0) != null) {
                String value = nodes.item(0).getTextContent();
                if (value != null && !value.isBlank()) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    private static String textOrNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static BigDecimal parseDecimal(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim().replace(',', '.'));
        } catch (Exception ex) {
            return null;
        }
    }

    private record Pair(String baseCurrency, String quoteCurrency) {}
}


    