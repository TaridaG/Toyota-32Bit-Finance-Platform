package com.company.marketdataservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "providers.finnhub")
public class FinnhubProperties {

    private boolean enabled = false;
    private String baseUrl = "https://finnhub.io";
    private String stockQuotePath = "/api/v1/quote";
    private String stockCandlePath = "/api/v1/stock/candle";
    private String apiKey = "";
    /** Symbols that Finnhub should own for historical ingestion (e.g. AAPL, AMZN, NVDA). */
    private List<String> symbols = new ArrayList<>();
}
