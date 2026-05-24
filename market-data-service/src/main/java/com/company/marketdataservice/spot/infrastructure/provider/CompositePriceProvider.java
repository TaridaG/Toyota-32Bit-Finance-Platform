package com.company.marketdataservice.spot.infrastructure.provider;
import com.company.marketdataservice.shared.resilience.ResilientPriceProvider;
import com.company.marketdataservice.bootstrap.config.ResilienceProperties;
import com.company.marketdataservice.shared.metrics.PriceProviderMetrics;
import com.company.marketdataservice.spot.infrastructure.provider.binance.BinancePriceProvider;
import com.company.marketdataservice.spot.infrastructure.provider.coingecko.CoinGeckoPriceProvider;
import com.company.marketdataservice.shared.provider.health.ProviderHealthTracker;
import com.company.marketdataservice.spot.infrastructure.provider.yahoo.YahooFinanceProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Spot price provider zincirini sırayla dener; ilk başarılı fiyatı döner.
 */
@Slf4j
@Component
@Primary
public class CompositePriceProvider implements PriceProvider {

    private final List<ResilientPriceProvider> resilientProviders;
    private final ProviderHealthTracker healthTracker;
    private final PriceProviderMetrics metrics;

    public CompositePriceProvider(
            List<PriceProvider> providers,
            ProviderHealthTracker healthTracker,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            ResilienceProperties resilienceProperties,
            ExecutorService resilienceExecutorService,
            PriceProviderMetrics metrics
    ) {
        this.healthTracker = healthTracker;
        this.metrics = metrics;

        this.resilientProviders = providers.stream()
                .filter(provider -> !(provider instanceof CompositePriceProvider))
                .sorted(Comparator.comparingInt(CompositePriceProvider::providerChainOrder))
                .map(provider -> new ResilientPriceProvider(
                        provider,
                        circuitBreakerRegistry,
                        retryRegistry,
                        resilienceProperties,
                        resilienceExecutorService
                ))
                .toList();
    }

    private static int providerChainOrder(PriceProvider p) {
        if (p instanceof YahooFinanceProvider) {
            return 0;
        }
        if (p instanceof CoinGeckoPriceProvider) {
            return 1;
        }
        if (p instanceof BinancePriceProvider) {
            return 2;
        }
        return 50;
    }

    /**
     * Harici kaynaktan veri fetch eder.
         * @param symbol enstrüman sembolü
         * @return işlem sonucu
         */
    @Override
    public BigDecimal fetchPrice(String symbol) {
        for (PriceProvider provider : resilientProviders) {
            var timer = metrics.startTimer();
            try {
                BigDecimal price = provider.fetchPrice(symbol);
                healthTracker.recordSuccess(provider.source());
                metrics.recordSuccess(provider.source());
                metrics.recordLatency(provider.source(), timer);

                log.info("PROVIDER_SUCCESS provider={}, symbol={}, price={}",
                        provider.source(), symbol, price);

                return price;

            } catch (Exception ex) {
                healthTracker.recordFailure(provider.source());
                metrics.recordFailure(provider.source());
                metrics.recordLatency(provider.source(), timer);

                log.warn("PROVIDER_FAILED provider={}, symbol={}, reason={}",
                        provider.source(), symbol, ex.getMessage());
            }
        }

        throw new RuntimeException("All providers failed for symbol=" + symbol);
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @Override
    public String source() {
        return "COMPOSITE";
    }
}