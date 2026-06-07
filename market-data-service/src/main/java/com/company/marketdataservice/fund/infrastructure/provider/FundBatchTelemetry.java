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
     * Son fon fetch batch'inde HTTP fallback kullanıldığını işaretler;
     * {@link com.company.marketdataservice.fund.infrastructure.scheduler.FundScheduler} özet logları için.
     */
    public void markHttpFallback() {
        lastBatchUsedHttpFallback.set(true);
    }

    /**
     * Son batch'te HTTP fallback kullanılıp kullanılmadığını okur ve flag'i sıfırlar (consume-on-read).
     *
     * @return bir önceki batch HTTP fallback ile tamamlandıysa {@code true}
     */
    public boolean consumeHttpFallback() {
        return lastBatchUsedHttpFallback.getAndSet(false);
    }
}
