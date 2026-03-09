package com.company.marketdataservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ResilienceRegistryConfig {

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(ResilienceProperties properties) {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

        properties.getProviders().forEach((providerName, providerConfig) -> {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .failureRateThreshold(providerConfig.getCircuitBreaker().getFailureRateThreshold())
                    .slidingWindowSize(providerConfig.getCircuitBreaker().getSlidingWindowSize())
                    .waitDurationInOpenState(Duration.ofMillis(
                            providerConfig.getCircuitBreaker().getWaitDurationInOpenStateMs()))
                    .permittedNumberOfCallsInHalfOpenState(
                            providerConfig.getCircuitBreaker().getPermittedNumberOfCallsInHalfOpenState())
                    .build();

            registry.addConfiguration(providerName, config);
        });

        return registry;
    }

    @Bean
    public RetryRegistry retryRegistry(ResilienceProperties properties) {
        RetryRegistry registry = RetryRegistry.ofDefaults();

        properties.getProviders().forEach((providerName, providerConfig) -> {
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(providerConfig.getRetry().getMaxAttempts())
                    .intervalFunction(IntervalFunction.of(providerConfig.getRetry().getDelayMs()))
                    .failAfterMaxAttempts(true)
                    .build();

            registry.addConfiguration(providerName, config);
        });

        return registry;
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService resilienceExecutorService() {
        return Executors.newFixedThreadPool(8, runnable -> {
            Thread t = new Thread(runnable);
            t.setName("market-data-resilience-worker");
            t.setDaemon(true);
            return t;
        });
    }
}