package com.company.marketdataservice.fx.infrastructure.provider.tcmb;
import com.company.marketdataservice.bootstrap.config.FxMarketProperties;
import com.company.marketdataservice.fx.domain.FxProvider;
import com.company.marketdataservice.fx.domain.FxQuoteNormalization;
import com.company.marketdataservice.fx.domain.FxSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * `FX spot` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Slf4j
@Component
public class TcmbFxProvider implements FxProvider {

    private static final String QUOTE_CCY = "TRY";

    private static final List<Charset> DECODE_ORDER = List.of(
            StandardCharsets.UTF_8,
            Charset.forName("ISO-8859-9"),
            Charset.forName("Windows-1254")
    );

    private final FxMarketProperties fxMarketProperties;
    private final WebClient fxWebClient;

    public TcmbFxProvider(FxMarketProperties fxMarketProperties, @Qualifier("fxWebClient") WebClient fxWebClient) {
        this.fxMarketProperties = fxMarketProperties;
        this.fxWebClient = fxWebClient;
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @return işlem sonucu
         */
    @Override
    public List<FxSnapshot> fetchLatestRates() {
        String baseUrl = fxMarketProperties.getTcmbUrl();
        String apiKey = fxMarketProperties.getTcmbApiKey();
        String finalUrl = appendApiKeyQueryIfConfigured(baseUrl, apiKey);
        byte[] body = fxWebClient.get()
                .uri(URI.create(finalUrl))
                .headers(headers -> {
                    if (StringUtils.hasText(apiKey) && StringUtils.hasText(fxMarketProperties.getTcmbApiKeyHeader())) {
                        headers.set(fxMarketProperties.getTcmbApiKeyHeader(), apiKey);
                    }
                })
                .accept(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.ALL)
                .retrieve()
                .bodyToMono(byte[].class)
                .block();
        if (body == null || body.length == 0) {
            return List.of();
        }
        for (Charset cs : DECODE_ORDER) {
            try {
                String text = new String(body, cs);
                List<FxSnapshot> parsed = parseXmlString(text);
                if (!parsed.isEmpty()) {
                    return parsed;
                }
            } catch (Exception ignored) {
            }
        }
        log.warn("TCMB_PARSE_EMPTY_OR_UNSUPPORTED source={} url={}", source(), baseUrl);
        return List.of();
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @Override
    public String source() {
        return "TCMB";
    }

    private List<FxSnapshot> parseXmlString(String xml) throws Exception {
        Set<String> wanted = fxMarketProperties.getProviderCurrencies().stream()
                .map(s -> s == null ? "" : s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        factory.setExpandEntityReferences(false);
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {
        }
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        NodeList currencies = doc.getElementsByTagName("Currency");
        List<FxSnapshot> out = new ArrayList<>();
        Instant ts = Instant.now();
        for (int i = 0; i < currencies.getLength(); i++) {
            if (!(currencies.item(i) instanceof Element el)) {
                continue;
            }
            String kod = textOrNull(el.getAttribute("Kod"));
            if (kod == null) {
                kod = textOrNull(el.getAttribute("CurrencyCode"));
            }
            if (kod == null) {
                continue;
            }
            kod = kod.trim().toUpperCase(Locale.ROOT);
            if (!wanted.contains(kod)) {
                continue;
            }
            BigDecimal bid = parseDecimal(firstNonEmpty(el, "ForexBuying", "BanknoteBuying"));
            BigDecimal ask = parseDecimal(firstNonEmpty(el, "ForexSelling", "BanknoteSelling"));
            if (bid == null || ask == null) {
                continue;
            }
            BigDecimal mid = bid.add(ask).divide(BigDecimal.valueOf(2), 6, RoundingMode.HALF_UP);
            String canonical = kod + QUOTE_CCY;
            bid = FxQuoteNormalization.normalizePrice(canonical, bid);
            ask = FxQuoteNormalization.normalizePrice(canonical, ask);
            mid = FxQuoteNormalization.normalizePrice(canonical, mid);
            out.add(new FxSnapshot(
                    canonical,
                    kod,
                    QUOTE_CCY,
                    bid,
                    ask,
                    mid,
                    ts,
                    source()
            ));
        }
        return out;
    }

    private static String firstNonEmpty(Element el, String... tags) {
        for (String t : tags) {
            NodeList nl = el.getElementsByTagName(t);
            if (nl.getLength() > 0 && nl.item(0) != null) {
                String v = nl.item(0).getTextContent();
                if (v != null && !v.isBlank()) {
                    return v.trim();
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
        String normalized = raw.trim().replace(',', '.');
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String appendApiKeyQueryIfConfigured(String url, String apiKey) {
        if (!StringUtils.hasText(url) || !StringUtils.hasText(apiKey) || !StringUtils.hasText(fxMarketProperties.getTcmbApiKeyQueryParam())) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + fxMarketProperties.getTcmbApiKeyQueryParam() + "=" + apiKey;
    }
}
