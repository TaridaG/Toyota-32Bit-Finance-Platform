package com.company.marketdataservice.provider;

import com.company.marketdataservice.provider.health.ProviderHealthTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class CompositePriceProvider implements PriceProvider {

    private final List<PriceProvider> providers;
    private final ProviderHealthTracker healthTracker;

    @Override
    public BigDecimal fetchPrice(String symbol) {

        for (PriceProvider provider : providers) {
            try {
                BigDecimal price = provider.fetchPrice(symbol);

                healthTracker.recordSuccess(provider.source());

                log.info("PROVIDER_SUCCESS provider={}, symbol={}, price={}",
                        provider.source(), symbol, price);

                return price;

            } catch (Exception e) {

                healthTracker.recordFailure(provider.source());

                log.warn("PROVIDER_FAILED provider={}, symbol={}, reason={}",
                        provider.source(), symbol, e.getMessage());
            }
        }
        throw new RuntimeException("All providers failed for symbol=" + symbol);
    }

    @Override
    public String source() {
        return "COMPOSITE";
    }
}
