package com.company.marketdataservice.provider.health;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ProviderHealthTracker {

    private final ConcurrentHashMap<String, ProviderMetrics> metrics = new ConcurrentHashMap<>();

    public void recordSuccess(String provider) {
        ProviderMetrics m = metrics.computeIfAbsent(provider, k -> new ProviderMetrics());
        m.success.incrementAndGet();
    }

    public void recordFailure(String provider) {
        ProviderMetrics m = metrics.computeIfAbsent(provider, k -> new ProviderMetrics());
        m.failure.incrementAndGet();
        m.lastFailure = Instant.now();
    }

    public ProviderMetrics getMetrics(String provider) {
        return metrics.get(provider);
    }

    @Getter
    public static class ProviderMetrics {
        private final AtomicInteger success = new AtomicInteger(0);
        private final AtomicInteger failure = new AtomicInteger(0);
        private Instant lastFailure;
    }
}
