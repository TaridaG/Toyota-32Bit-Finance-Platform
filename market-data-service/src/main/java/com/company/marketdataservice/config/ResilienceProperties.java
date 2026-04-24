package com.company.marketdataservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "resilience")
public class ResilienceProperties {

    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Data
    public static class ProviderConfig {
        private Retry retry = new Retry();
        private Timeout timeout = new Timeout();
        private CircuitBreaker circuitBreaker = new CircuitBreaker();
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long delayMs = 500;
    }

    @Data
    public static class Timeout {
        private long millis = 3000;
    }

    @Data
    public static class CircuitBreaker {
        private float failureRateThreshold = 50f;
        private int slidingWindowSize = 10;
        private long waitDurationInOpenStateMs = 30000;
        private int permittedNumberOfCallsInHalfOpenState = 3;
    }

    public ProviderConfig getRequiredProvider(String providerName) {
        ProviderConfig config = providers.get(providerName.toLowerCase(Locale.ROOT));
        if (config == null) {
            throw new IllegalStateException("Missing resilience config for provider: " + providerName);
        }
        return config;
    }
}