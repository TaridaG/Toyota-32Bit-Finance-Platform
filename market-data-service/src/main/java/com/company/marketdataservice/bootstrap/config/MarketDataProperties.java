package com.company.marketdataservice.bootstrap.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Root market-data-service configuration (schedulers, ingestion env).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "market")
public class MarketDataProperties {

    private String provider;
    private Ingestion ingestion = new Ingestion();

    @Getter
    @Setter
    public static class Ingestion {
        private String env = "dev";
    }
}
