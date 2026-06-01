package com.company.marketdataservice.bootstrap.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code market.*} kök yapılandırması: varsayılan provider ve ingestion ortamı ({@code dev}/{@code prod}).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "market")
public class MarketDataProperties {

    private String provider;
    private Ingestion ingestion = new Ingestion();

    /** Ingest ortam etiketi; provider davranışında ortam ayrımı için kullanılır. */
    @Getter
    @Setter
    public static class Ingestion {
        private String env = "dev";
    }
}
