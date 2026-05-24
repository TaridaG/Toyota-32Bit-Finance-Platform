package com.company.marketdataservice.shared.provider.tcmb;
import com.company.marketdataservice.fx.infrastructure.provider.evds.EvdsHistoricalFxProvider;
import com.company.marketdataservice.bootstrap.config.MarketEvdsProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * `ortak altyapı` harici HTTP/API client adaptörü.
 */
@Component
public class TcmbBondEvdsClient {

    private static final DateTimeFormatter EVDS_DATE = DateTimeFormatter.ofPattern("dd-MM-uuuu");

    private final MarketEvdsProperties evdsProperties;
    private final ObjectMapper objectMapper;
    private final WebClient fxWebClient;

    public TcmbBondEvdsClient(
            MarketEvdsProperties evdsProperties,
            ObjectMapper objectMapper,
            @Qualifier("fxWebClient") WebClient fxWebClient
    ) {
        this.evdsProperties = evdsProperties;
        this.objectMapper = objectMapper;
        this.fxWebClient = fxWebClient;
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param evdsSeries girdi parametresi
         * @return işlem sonucu
         */
    public BigDecimal fetchLatestValue(String evdsSeries) {
        if (!StringUtils.hasText(evdsSeries)) {
            throw new IllegalArgumentException("evds series is blank");
        }
        if (!StringUtils.hasText(evdsProperties.getApiKey())) {
            throw new IllegalStateException("EVDS API key is not configured");
        }
        String series = evdsSeries.trim().toUpperCase(Locale.ROOT);
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);
        String uri = buildRangeUri(series, start, end, null);
        try {
            String body = fxWebClient.get()
                    .uri(uri)
                    .header("key", evdsProperties.getApiKey())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (!StringUtils.hasText(body)) {
                throw new IllegalStateException("EVDS response is empty");
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                throw new IllegalStateException("EVDS items are empty");
            }
            for (int i = items.size() - 1; i >= 0; i--) {
                JsonNode item = items.get(i);
                BigDecimal parsed = parseBondValue(item, series);
                if (parsed != null && parsed.compareTo(BigDecimal.ZERO) > 0) {
                    return parsed;
                }
            }
            throw new IllegalStateException("EVDS did not return a valid numeric point for series=" + evdsSeries);
        } catch (Exception ex) {
            throw new IllegalStateException("EVDS bond fetch failed series=" + evdsSeries, ex);
        }
    }

    /**
     * EVDS observations for {@code [startDate, endDate]} inclusive.
     * Optional {@code evdsFrequency}: e.g. {@code "1"} daily, {@code "2"} weekly (EVDS kodları); {@code null} omits the query param.
     */
    public List<BondEodPoint> fetchHistoryPoints(String evdsSeries, LocalDate startDate, LocalDate endDate) {
        return fetchHistoryPoints(evdsSeries, startDate, endDate, null);
    }

    /**
     * Same as {@link #fetchHistoryPoints(String, LocalDate, LocalDate)} with optional EVDS {@code frequency}
     * (e.g. {@code 5} monthly for {@code TP_BISPOLFAIZ_TUR}).
     */
    public List<BondEodPoint> fetchHistoryPoints(String evdsSeries, LocalDate startDate, LocalDate endDate, String evdsFrequency) {
        if (!StringUtils.hasText(evdsSeries) || startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return List.of();
        }
        if (!StringUtils.hasText(evdsProperties.getApiKey())) {
            throw new IllegalStateException("EVDS API key is not configured");
        }
        String series = evdsSeries.trim().toUpperCase(Locale.ROOT);
        String uri = buildRangeUri(series, startDate, endDate, evdsFrequency);
        try {
            String body = fxWebClient.get()
                    .uri(uri)
                    .header("key", evdsProperties.getApiKey())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            if (!StringUtils.hasText(body)) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                return List.of();
            }
            List<BondEodPoint> out = new ArrayList<>();
            for (JsonNode item : items) {
                LocalDate d = parseEvdsDate(item.path("Tarih").asText(null));
                if (d == null) {
                    continue;
                }
                BigDecimal parsed = parseBondValue(item, series);
                if (parsed != null && parsed.compareTo(BigDecimal.ZERO) >= 0) {
                    out.add(new BondEodPoint(d, parsed));
                }
            }
            out.sort(Comparator.comparing(BondEodPoint::date));
            return out;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "EVDS bond history fetch failed series=" + evdsSeries + " range=" + startDate + ".." + endDate,
                    ex
            );
        }
    }

    private String buildRangeUri(String series, LocalDate start, LocalDate end, String evdsFrequency) {
        String base = evdsProperties.getBaseUrl();
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException("EVDS base url is not configured");
        }
        // Same path shape as {@link com.company.marketdataservice.fx.infrastructure.provider.evds.EvdsHistoricalFxProvider}: host + /series=CODE&...
        // Series code must stay dotted (do not URL-encode dots) — EVDS v3 returns columns like TP_KTF10 in JSON.
        String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        // EVDS path requires dotted series codes (TP.BISPOLFAIZ.TUR, TP.KTF10). Underscore form (TP_BISPOLFAIZ_TUR) returns HTTP 400.
        String seriesInUrl = series.replace('_', '.');
        String uri =
                normalized
                + "/series="
                + seriesInUrl
                + "&startDate="
                + encode(EVDS_DATE.format(start))
                + "&endDate="
                + encode(EVDS_DATE.format(end))
                + "&type=json";
        if (StringUtils.hasText(evdsFrequency)) {
            uri += "&frequency=" + evdsFrequency.trim();
        }
        return uri;
    }

    private static LocalDate parseEvdsDate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String t = raw.trim();
        try {
            return LocalDate.parse(t, EVDS_DATE);
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(t, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
        }
        try {
            if (t.matches("\\d{4}-\\d{1,2}-\\d{1,2}")) {
                String[] p = t.split("-");
                int y = Integer.parseInt(p[0]);
                int mo = Integer.parseInt(p[1]);
                int d = Integer.parseInt(p[2]);
                return LocalDate.of(y, mo, d);
            }
        } catch (Exception ignored) {
        }
        try {
            if (t.matches("\\d{4}-\\d{1,2}")) {
                String[] p = t.split("-");
                int y = Integer.parseInt(p[0]);
                int mo = Integer.parseInt(p[1]);
                return LocalDate.of(y, mo, 1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    static BigDecimal parseBondValue(JsonNode item, String evdsSeries) {
        // Only the requested series column. EVDS multi-series rows often include TP_KANUNI_FAIZ_ORAN (kanuni faiz);
        // if the policy cell (e.g. TP_BISPOLFAIZ_TUR / TP_FG_J0) is blank we must NOT fall back to it (yields ~3.683 instead of ~37).
        String canon = evdsSeries.trim().toUpperCase(Locale.ROOT);
        String underscored = canon.replace('.', '_');
        String raw = firstNonEmpty(item, underscored, canon);
        return EvdsTurkishNumberParser.parsePercentLike(raw);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String firstNonEmpty(JsonNode node, String... names) {
        for (String n : names) {
            JsonNode v = node.get(n);
            if (v != null && !v.isNull()) {
                String s = v.asText();
                if (StringUtils.hasText(s)) {
                    return s.trim();
                }
            }
        }
        return null;
    }

}
