package com.company.marketdataservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "market")
public class MarketDataProperties {

    private List<String> trackedSymbols;
    private List<String> trackedStocks = new ArrayList<>();
    /** Alias list for UX; reuse same YAML anchor as {@code fund.tracked-fund-codes}. */
    private List<String> trackedFunds = new ArrayList<>();
    private String provider;
    private Ingestion ingestion = new Ingestion();

    @Getter
    @Setter
    public static class Ingestion {
        private String env = "dev";
    }
}
