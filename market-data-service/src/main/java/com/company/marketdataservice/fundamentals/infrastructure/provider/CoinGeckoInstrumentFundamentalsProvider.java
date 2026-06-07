package com.company.marketdataservice.fundamentals.infrastructure.provider;
import com.fasterxml.jackson.databind.JsonNode;
import com.company.marketdataservice.catalog.registry.providers.CryptoRegistry;
import com.company.marketdataservice.fundamentals.infrastructure.http.dto.InstrumentFundamentalsDto;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
/**
 * `temel veri (fundamentals)` harici veri kaynağından fiyat veya snapshot fetch eden provider adaptörü.
 */
@Component
public class CoinGeckoInstrumentFundamentalsProvider implements InstrumentFundamentalsProvider {
    private static final String PROVIDER = "COINGECKO";
    private final RestClient restClient = RestClient.create();

    @Value("${providers.coingecko.base-url:https://api.coingecko.com/api/v3}")
    private String baseUrl;

    /**
     * Provider kodunu ({@code COINGECKO}) döndürür.
         */
    @Override
    public String providerCode() {
        return PROVIDER;
    }

    /**
     * Enstrümanın CRYPTO asset class'ına ait olup olmadığını kontrol eder.
         * @param instrument katalog kaydı
         */
    @Override
    public boolean supports(InstrumentCatalogEntry instrument) {
        return "CRYPTO".equalsIgnoreCase(instrument.getAssetClass());
    }

    /**
     * CoinGecko REST API üzerinden kripto enstrüman fundamentals snapshot'ını fetch eder.
         * @param instrument katalog kaydı
         * @param providerSymbol CoinGecko API için provider sembolü
         * @return fundamentals DTO
         */
    @Override
    public InstrumentFundamentalsDto fetch(InstrumentCatalogEntry instrument, String providerSymbol) {
        String normalized = normalize(providerSymbol);
        String ticker = normalized.endsWith("USDT") ? normalized.substring(0, normalized.length() - 4) : normalized;
        String id = CryptoRegistry.coingeckoIdForBase(ticker);
        if (id == null) {
            id = ticker.toLowerCase(Locale.ROOT);
        }
        JsonNode coin = getJson("/coins/" + id + "?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false&sparkline=false");
        if (coin == null || coin.isMissingNode() || coin.path("id").isMissingNode()) {
            throw new IllegalStateException("CoinGecko metadata not found for symbol=" + providerSymbol);
        }
        JsonNode marketData = coin.path("market_data");
        return new InstrumentFundamentalsDto(
                instrument.getCanonicalSymbol(),
                PROVIDER,
                providerSymbol,
                text(coin, "name"),
                text(coin, "country_origin"),
                "USD",
                "CRYPTO",
                text(coin, "genesis_date"),
                firstArrayValue(coin.path("categories")),
                firstArrayValue(coin.path("links").path("homepage")),
                decimal(marketData.path("market_cap").path("usd")),
                decimal(marketData.path("circulating_supply")),
                null,
                decimal(marketData.path("current_price").path("usd")),
                Instant.now(),
                false,
                java.util.List.of()
        );
    }

    private JsonNode getJson(String pathAndQuery) {
        String root = baseUrl.replaceAll("/+$", "");
        return restClient.get()
                .uri(root + pathAndQuery)
                .retrieve()
                .body(JsonNode.class);
    }

    private static String firstArrayValue(JsonNode node) {
        if (node == null || !node.isArray() || node.isEmpty()) {
            return null;
        }
        String value = node.get(0).asText(null);
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.path(field).isMissingNode() || node.path(field).isNull()) {
            return null;
        }
        String value = node.path(field).asText(null);
        return (value == null || value.isBlank()) ? null : value;
    }

    private static BigDecimal decimal(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            if (node.isNumber()) {
                return node.decimalValue();
            }
            String raw = node.asText();
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return new BigDecimal(raw);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("providerSymbol is blank");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}

