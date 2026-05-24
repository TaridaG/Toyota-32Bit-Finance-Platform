package com.company.marketdataservice.bootstrap.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * `uygulama bootstrap` feature yapılandırma property'leri (`application.yml` prefix).
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
    /** Symbols that Finnhub should own for historical ingestion (e.g. AAPL, AMZN, NVDA). */
    private List<String> symbols = new ArrayList<>();
}
