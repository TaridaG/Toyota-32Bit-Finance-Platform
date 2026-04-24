package com.company.marketdataservice.fund;

import com.company.marketdataservice.config.FundMarketProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TefasHttpProvider {

    private static final String SRC = "TEFAS_HTTP";
    private static final Duration BLOCK = Duration.ofSeconds(20);

    private final FundMarketProperties fundMarketProperties;
    private final ObjectMapper objectMapper;

    @Qualifier("tefasWebClient")
    private final WebClient tefasWebClient;

    public boolean isConfigured() {
        String template = fundMarketProperties.getHttpUrlTemplate();
        return template != null && !template.isBlank();
    }

    public List<FundSnapshot> fetchLatestNavs(List<String> fundCodes) {
        String template = fundMarketProperties.getHttpUrlTemplate();
        if (template == null || template.isBlank() || fundCodes == null || fundCodes.isEmpty()) {
            return List.of();
        }
        Instant ts = Instant.now();
        List<FundSnapshot> out = new ArrayList<>();
        for (String raw : fundCodes) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String code = raw.trim().toUpperCase(Locale.ROOT);
            try {
                String uri = template.replace("{code}", java.net.URLEncoder.encode(code, java.nio.charset.StandardCharsets.UTF_8));
                String body = tefasWebClient.get()
                        .uri(uri)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(BLOCK);
                if (body == null || body.isBlank()) {
                    continue;
                }
                JsonNode root = objectMapper.readTree(body);
                Optional<BigDecimal> nav = findNav(root);
                if (nav.isEmpty()) {
                    continue;
                }
                out.add(new FundSnapshot(null, code, nav.get(), ts, SRC));
            } catch (WebClientResponseException ignored) {
                continue;
            } catch (Exception ignored) {
                continue;
            }
        }
        return out;
    }

    private static Optional<BigDecimal> findNav(JsonNode node) {
        if (node == null || node.isNull()) {
            return Optional.empty();
        }
        if (node.isNumber()) {
            return Optional.of(node.decimalValue());
        }
        if (node.isTextual()) {
            try {
                return Optional.of(new BigDecimal(node.asText().replace(',', '.').trim()));
            } catch (Exception ex) {
                return Optional.empty();
            }
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String k = e.getKey().toLowerCase(Locale.ROOT);
                if (k.contains("nav") || k.contains("fiyat") || k.equals("price") || k.contains("birimfiyat")) {
                    Optional<BigDecimal> v = findNav(e.getValue());
                    if (v.isPresent()) {
                        return v;
                    }
                }
            }
            for (JsonNode child : node) {
                Optional<BigDecimal> v = findNav(child);
                if (v.isPresent()) {
                    return v;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                Optional<BigDecimal> v = findNav(child);
                if (v.isPresent()) {
                    return v;
                }
            }
        }
        return Optional.empty();
    }
}
