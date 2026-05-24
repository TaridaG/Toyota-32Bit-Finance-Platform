package com.company.marketdataservice.spot.infrastructure.provider;

import com.company.marketdataservice.bootstrap.config.ResilienceProperties;
import com.company.marketdataservice.shared.metrics.PriceProviderMetrics;
import com.company.marketdataservice.shared.provider.health.ProviderHealthTracker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CompositePriceProviderTest {

    private record ResilienceBundle(
            ResilienceProperties properties,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
    }

    private static ResilienceBundle resilienceFor(String... sources) {
        ResilienceProperties props = new ResilienceProperties();
        Map<String, ResilienceProperties.ProviderConfig> map = new HashMap<>();
        for (String source : sources) {
            map.put(source.toLowerCase(Locale.ROOT), new ResilienceProperties.ProviderConfig());
        }
        props.setProviders(map);

        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        map.forEach((providerName, providerConfig) -> {
            cbRegistry.addConfiguration(providerName, CircuitBreakerConfig.custom()
                    .failureRateThreshold(providerConfig.getCircuitBreaker().getFailureRateThreshold())
                    .slidingWindowSize(providerConfig.getCircuitBreaker().getSlidingWindowSize())
                    .waitDurationInOpenState(Duration.ofMillis(
                            providerConfig.getCircuitBreaker().getWaitDurationInOpenStateMs()))
                    .permittedNumberOfCallsInHalfOpenState(
                            providerConfig.getCircuitBreaker().getPermittedNumberOfCallsInHalfOpenState())
                    .build());
            retryRegistry.addConfiguration(providerName, RetryConfig.custom()
                    .maxAttempts(providerConfig.getRetry().getMaxAttempts())
                    .intervalFunction(IntervalFunction.of(providerConfig.getRetry().getDelayMs()))
                    .failAfterMaxAttempts(true)
                    .build());
        });
        return new ResilienceBundle(props, cbRegistry, retryRegistry);
    }

    @Test
    void fetchPrice_returnsFirstSuccessfulProvider() {
        PriceProvider primary = stubProvider("PRIMARY", "AAPL", new BigDecimal("100"));
        PriceProvider backup = stubProvider("BACKUP", "AAPL", new BigDecimal("99"));
        ResilienceBundle resilience = resilienceFor("PRIMARY", "BACKUP");

        CompositePriceProvider composite = new CompositePriceProvider(
                List.of(primary, backup),
                new ProviderHealthTracker(),
                resilience.circuitBreakerRegistry(),
                resilience.retryRegistry(),
                resilience.properties(),
                Executors.newVirtualThreadPerTaskExecutor(),
                new PriceProviderMetrics(new SimpleMeterRegistry())
        );

        assertEquals(0, new BigDecimal("100").compareTo(composite.fetchPrice("AAPL")));
    }

    @Test
    void fetchPrice_fallsThroughWhenFirstProviderFails() {
        PriceProvider failing = new PriceProvider() {
            @Override
            public BigDecimal fetchPrice(String symbol) {
                throw new RuntimeException("down");
            }

            @Override
            public String source() {
                return "FAIL";
            }
        };
        PriceProvider backup = stubProvider("BACKUP", "AAPL", new BigDecimal("50"));
        ResilienceBundle resilience = resilienceFor("FAIL", "BACKUP");

        CompositePriceProvider composite = new CompositePriceProvider(
                List.of(failing, backup),
                new ProviderHealthTracker(),
                resilience.circuitBreakerRegistry(),
                resilience.retryRegistry(),
                resilience.properties(),
                Executors.newVirtualThreadPerTaskExecutor(),
                new PriceProviderMetrics(new SimpleMeterRegistry())
        );

        assertEquals(0, new BigDecimal("50").compareTo(composite.fetchPrice("AAPL")));
    }

    @Test
    void fetchPrice_throwsWhenAllProvidersFail() {
        PriceProvider failing = new PriceProvider() {
            @Override
            public BigDecimal fetchPrice(String symbol) {
                throw new RuntimeException("down");
            }

            @Override
            public String source() {
                return "FAIL";
            }
        };

        ResilienceBundle resilience = resilienceFor("FAIL");

        CompositePriceProvider composite = new CompositePriceProvider(
                List.of(failing),
                new ProviderHealthTracker(),
                resilience.circuitBreakerRegistry(),
                resilience.retryRegistry(),
                resilience.properties(),
                Executors.newVirtualThreadPerTaskExecutor(),
                new PriceProviderMetrics(new SimpleMeterRegistry())
        );

        assertThrows(RuntimeException.class, () -> composite.fetchPrice("AAPL"));
    }

    private static PriceProvider stubProvider(String source, String symbol, BigDecimal price) {
        return new PriceProvider() {
            @Override
            public BigDecimal fetchPrice(String requested) {
                if (!symbol.equals(requested)) {
                    throw new RuntimeException("unexpected symbol");
                }
                return price;
            }

            @Override
            public String source() {
                return source;
            }
        };
    }
}
