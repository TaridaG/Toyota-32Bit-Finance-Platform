package com.company.marketdataservice.provider;

import com.company.marketdataservice.config.ResilienceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@RequiredArgsConstructor
public class ResilientPriceProvider implements PriceProvider {

    private final PriceProvider delegate;
    private final ResilienceProperties properties;

    @Override
    public BigDecimal fetchPrice(String symbol) {

        int attempts = 0;
        int max = properties.getRetry().getMaxAttempts();

        while (true) {
            try {

                attempts++;
                return delegate.fetchPrice(symbol);

            } catch (Exception e) {

                if (attempts >= max) {
                    log.error("PRICE_FETCH_FAILED provider={}, symbol={}, attempts={}",
                            delegate.source(), symbol, attempts);
                    throw e;
                }

                log.warn("PRICE_FETCH_RETRY provider={}, symbol={}, attempt={}",
                        delegate.source(), symbol, attempts);

                try {
                    Thread.sleep(properties.getRetry().getDelayMs());
                } catch (InterruptedException ignored) {}
            }
        }
    }

    @Override
    public String source() {
        return delegate.source();
    }
}
