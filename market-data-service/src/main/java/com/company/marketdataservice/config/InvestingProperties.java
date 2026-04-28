package com.company.marketdataservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "providers.investing")
public class InvestingProperties {

    private String baseUrl = "https://api.investing.com";

    private String pricePathTemplate = "/api/financialdata/{symbol}/price";

    private String userAgent = "Mozilla/5.0 (compatible; FinanceMarketDataService/1.0)";
}
