package com.company.marketdataservice.provider;

import com.company.marketdataservice.config.ResilienceProperties;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
public class ResilientPriceProvider implements PriceProvider {

    private final PriceProvider delegate;
    private final io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker;
    private final io.github.resilience4j.retry.Retry retry;
    private final ExecutorService executorService;
    private final long timeoutMillis;

    public ResilientPriceProvider(
            PriceProvider delegate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            ResilienceProperties resilienceProperties,
            ExecutorService executorService
    ) {
        this.delegate = delegate;
        this.executorService = executorService;

        String providerKey = delegate.source().toLowerCase();
        ResilienceProperties.ProviderConfig providerConfig =
                resilienceProperties.getRequiredProvider(providerKey);

        this.timeoutMillis = providerConfig.getTimeout().getMillis();

        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(providerKey, providerKey);
        this.retry = retryRegistry.retry(providerKey, providerKey);
    }

    @Override
    public String source() {
        return delegate.source();
    }

    @Override
    public BigDecimal fetchPrice(String symbol) {
        Supplier<BigDecimal> timeoutProtectedSupplier = () -> executeWithTimeout(symbol);
        Supplier<BigDecimal> retryableSupplier =
                io.github.resilience4j.retry.Retry.decorateSupplier(retry, timeoutProtectedSupplier);
        Supplier<BigDecimal> protectedSupplier =
                io.github.resilience4j.circuitbreaker.CircuitBreaker.decorateSupplier(
                        circuitBreaker,
                        retryableSupplier
                );

        try {
            BigDecimal result = protectedSupplier.get();
            log.debug("PRICE_FETCH_SUCCESS provider={}, symbol={}", delegate.source(), symbol);
            return result;
        } catch (Exception ex) {
            Throwable root = unwrap(ex);
            log.warn("PRICE_FETCH_FAILED provider={}, symbol={}, reason={}",
                    delegate.source(), symbol, root.getMessage());
            throw new RuntimeException(
                    "Resilient fetch failed for provider=" + delegate.source() + ", symbol=" + symbol,
                    root
            );
        }
    }

    private BigDecimal executeWithTimeout(String symbol) {
        Future<BigDecimal> future = executorService.submit(() -> delegate.fetchPrice(symbol));
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new RuntimeException(
                    "Timeout while fetching price from provider=" + delegate.source() + ", symbol=" + symbol,
                    ex
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Interrupted while fetching price from provider=" + delegate.source() + ", symbol=" + symbol,
                    ex
            );
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(cause);
        }
    }

    private Throwable unwrap(Throwable ex) {
        if (ex instanceof CompletionException || ex instanceof ExecutionException) {
            return ex.getCause() != null ? ex.getCause() : ex;
        }
        return ex;
    }
}