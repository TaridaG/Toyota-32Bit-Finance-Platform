package com.company.marketdataservice.provider.investing;

import com.company.marketdataservice.config.InvestingProperties;
import com.company.marketdataservice.provider.PriceProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Component("investingStock")
public class InvestingStockPriceProvider implements PriceProvider {

    private static final Duration BLOCK = Duration.ofSeconds(20);

    private final InvestingProperties investingProperties;
    private final ObjectMapper objectMapper;
    private final WebClient investingStockWebClient;

    public InvestingStockPriceProvider(
            InvestingProperties investingProperties,
            ObjectMapper objectMapper,
            @Qualifier("investingStockWebClient") WebClient investingStockWebClient
    ) {
        this.investingProperties = investingProperties;
        this.objectMapper = objectMapper;
        this.investingStockWebClient = investingStockWebClient;
    }

    @Override
    public String source() {
        return "INVESTING_STOCK";
    }

    @Override
    public BigDecimal fetchPrice(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalStateException("symbol is blank");
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        String encoded = URLEncoder.encode(normalized, StandardCharsets.UTF_8);
        String path = investingProperties.getPricePathTemplate().replace("{symbol}", encoded);
        String base = investingProperties.getBaseUrl().replaceAll("/+$", "");
        String pathPart = path.startsWith("/") ? path : "/" + path;
        String uri = base + pathPart;

        String body;
        try {
            body = investingStockWebClient.get()
                    .uri(uri)
                    .header(HttpHeaders.USER_AGENT, investingProperties.getUserAgent())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(BLOCK);
        } catch (WebClientResponseException ex) {
            throw new IllegalStateException("Investing HTTP " + ex.getStatusCode().value() + " for uri=" + uri, ex);
        }

        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Investing empty body for symbol=" + normalized);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception ex) {
            throw new IllegalStateException("Investing invalid JSON for symbol=" + normalized, ex);
        }

        return extractPrice(root)
                .orElseThrow(() -> new IllegalStateException("Investing price field not found for symbol=" + normalized));
    }

    static Optional<BigDecimal> extractPrice(JsonNode root) {
        if (root == null || root.isNull()) {
            return Optional.empty();
        }
        if (root.isNumber()) {
            return Optional.of(root.decimalValue());
        }
        if (root.isTextual()) {
            try {
                return Optional.of(new BigDecimal(root.asText().replace(',', '.').trim()));
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
        if (root.isObject()) {
            for (String key : new String[] {"last", "Last", "close", "Close", "price", "Price", "last_close"}) {
                if (root.has(key) && !root.get(key).isNull()) {
                    Optional<BigDecimal> v = extractPrice(root.get(key));
                    if (v.isPresent()) {
                        return v;
                    }
                }
            }
            JsonNode data = root.get("data");
            if (data != null && !data.isNull()) {
                Optional<BigDecimal> v = extractPrice(data);
                if (v.isPresent()) {
                    return v;
                }
            }
        }
        return Optional.empty();
    }
}
