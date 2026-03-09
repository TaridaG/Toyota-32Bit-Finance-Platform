package com.company.marketdataservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PriceProviderMetrics {

    private final MeterRegistry meterRegistry;

    private final ConcurrentMap<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Counter> failureCounters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> latencyTimers = new ConcurrentHashMap<>();

    public PriceProviderMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordSuccess(String provider) {
        successCounters
                .computeIfAbsent(provider, p ->
                        Counter.builder("price_provider_success_total")
                                .tag("provider", p)
                                .register(meterRegistry))
                .increment();
    }

    public void recordFailure(String provider) {
        failureCounters
                .computeIfAbsent(provider, p ->
                        Counter.builder("price_provider_failure_total")
                                .tag("provider", p)
                                .register(meterRegistry))
                .increment();
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordLatency(String provider, Timer.Sample sample) {
        Timer timer = latencyTimers
                .computeIfAbsent(provider, p ->
                        Timer.builder("price_provider_latency")
                                .tag("provider", p)
                                .register(meterRegistry));
        sample.stop(timer);
    }
}