package com.company.marketdataservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "resilience")
public class ResilienceProperties {

    private Retry retry = new Retry();
    private Timeout timeout = new Timeout();

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long delayMs = 500;
    }

    @Data
    public static class Timeout {
        private int seconds = 3;
    }
}
