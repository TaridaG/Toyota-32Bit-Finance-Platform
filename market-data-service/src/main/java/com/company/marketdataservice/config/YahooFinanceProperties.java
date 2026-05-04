package com.company.marketdataservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "providers.yahoo")
public class YahooFinanceProperties {

    private String baseUrl = "https://query1.finance.yahoo.com";
    private String chartPathTemplate = "/v8/finance/chart/{symbol}";
    private String userAgent = "Mozilla/5.0 (compatible; FinanceMarketDataService/1.0)";
    private String defaultRange = "1d";
    private String defaultInterval = "1m";
}
