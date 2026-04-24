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

    private String tcmbUrl = "https://www.tcmb.gov.tr/kurlar/today.xml";
}
