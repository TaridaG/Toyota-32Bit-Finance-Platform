package com.company.marketdataservice.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "market")
public class MarketDataProperties {

    @Getter(AccessLevel.NONE)
    @Setter
    private List<String> trackedSymbols;

    /** Defaults to {@link TrackedCryptoSymbols#SYMBOLS} when YAML omits or empties the list. */
    public List<String> getTrackedSymbols() {
        if (trackedSymbols == null || trackedSymbols.isEmpty()) {
            return TrackedCryptoSymbols.SYMBOLS;
        }
        return trackedSymbols;
    }
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
