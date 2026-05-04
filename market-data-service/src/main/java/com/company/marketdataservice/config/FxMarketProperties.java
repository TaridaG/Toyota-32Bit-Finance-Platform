package com.company.marketdataservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "market.fx")
public class FxMarketProperties {

    private List<String> providerCurrencies = new ArrayList<>(List.of("USD", "EUR"));

    private List<String> providerOrder = new ArrayList<>(List.of("TCMB", "EXCHANGE_API"));

    private String tcmbUrl = "https://www.tcmb.gov.tr/kurlar/today.xml";
    private String tcmbApiKey;
    private String tcmbApiKeyQueryParam = "key";
    private String tcmbApiKeyHeader = "X-API-Key";

    private String exchangeRateUrl = "https://open.er-api.com/v6/latest/TRY";
}
