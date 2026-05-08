package com.company.marketdataservice.provider.tcmb;

import com.company.marketdataservice.config.MarketEvdsProperties;
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
import java.util.Locale;

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

    public BigDecimal fetchLatestValue(String evdsSeries) {
        if (!StringUtils.hasText(evdsSeries)) {
            throw new IllegalArgumentException("evds series is blank");
        }
        if (!StringUtils.hasText(evdsProperties.getApiKey())) {
            throw new IllegalStateException("EVDS API key is not configured");
        }
        String uri = buildLatestUri(evdsSeries.trim().toUpperCase(Locale.ROOT));
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
                String raw = firstNonEmpty(item, "TP_KANUNI_FAIZ_ORAN", "value", evdsSeries.replace('.', '_'));
                BigDecimal parsed = parseDecimal(raw);
                if (parsed != null && parsed.compareTo(BigDecimal.ZERO) > 0) {
                    return parsed;
                }
            }
            throw new IllegalStateException("EVDS did not return a valid numeric point for series=" + evdsSeries);
        } catch (Exception ex) {
            throw new IllegalStateException("EVDS bond fetch failed series=" + evdsSeries, ex);
        }
    }

    private String buildLatestUri(String series) {
        String base = evdsProperties.getBaseUrl();
        if (!StringUtils.hasText(base)) {
            throw new IllegalStateException("EVDS base url is not configured");
        }
        String normalized = base.endsWith("/") ? base : base + "/";
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);
        String params = "series=" + encode(series)
                + "&startDate=" + encode(EVDS_DATE.format(start))
                + "&endDate=" + encode(EVDS_DATE.format(end))
                + "&type=json";
        return normalized + params;
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

    private static BigDecimal parseDecimal(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return new BigDecimal(raw.trim().replace(',', '.'));
        } catch (Exception ex) {
            return null;
        }
    }
}
