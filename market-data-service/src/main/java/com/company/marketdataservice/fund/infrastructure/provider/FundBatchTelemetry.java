package com.company.marketdataservice.fund.infrastructure.provider;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * `fon (TEFAS NAV)` infrastructure katmanı adaptörü.
 */
@Component
public class FundBatchTelemetry {

    private final AtomicBoolean lastBatchUsedHttpFallback = new AtomicBoolean(false);

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    public void markHttpFallback() {
        lastBatchUsedHttpFallback.set(true);
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    public boolean consumeHttpFallback() {
        return lastBatchUsedHttpFallback.getAndSet(false);
    }
}
