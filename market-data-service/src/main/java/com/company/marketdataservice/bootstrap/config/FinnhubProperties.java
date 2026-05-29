package com.company.marketdataservice.bootstrap.config;

import com.company.marketdataservice.catalog.registry.providers.NasdaqRegistry;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Finnhub API configuration. Symbol ownership is resolved via {@link NasdaqRegistry}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "providers.finnhub")
public class FinnhubProperties {

    private boolean enabled = false;
    private String baseUrl = "https://finnhub.io";
    private String stockQuotePath = "/api/v1/quote";
    private String stockCandlePath = "/api/v1/stock/candle";
    private String stockProfilePath = "/api/v1/stock/profile2";
    private String stockFinancialsReportedPath = "/api/v1/stock/financials-reported";
    private String stockMetricPath = "/api/v1/stock/metric";
    private String apiKey = "";
    /** Optional YAML override; when empty, {@link NasdaqRegistry} is the source of truth. */
    private List<String> symbols = new ArrayList<>();

    public boolean ownsSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        if (symbols != null && !symbols.isEmpty()) {
            String normalized = symbol.trim().toUpperCase(Locale.ROOT);
            return symbols.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.trim().toUpperCase(Locale.ROOT))
                    .anyMatch(normalized::equals);
        }
        return NasdaqRegistry.isFinnhubOwned(symbol);
    }
}
