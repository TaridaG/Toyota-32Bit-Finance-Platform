package com.company.marketdataservice.history.infrastructure.orchestration;
import com.company.marketdataservice.bootstrap.config.MarketHistoryBackfillProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Backfill retry gecikmesi ve deneme üst sınırını hesaplar.
 */
@Component
public class IngestionRetryPolicy {

    private final MarketHistoryBackfillProperties backfillProperties;

    public IngestionRetryPolicy(MarketHistoryBackfillProperties backfillProperties) {
        this.backfillProperties = backfillProperties;
    }

    /**
     * Bir sonraki retry zamanını hesaplar.
         * @param attemptCount deneme sayısı
         * @param now referans zaman damgası
         * @return işlem sonucu
         */
    public Instant nextRetryAt(long attemptCount, Instant now) {
        return now.plus(computeDelay(attemptCount));
    }

    /**
     * Retry yapılıp yapılamayacağını belirler.
         * @param attemptCount deneme sayısı
         * @return işlem sonucu
         */
    public boolean canRetry(long attemptCount) {
        int maxAttempts = Math.max(1, backfillProperties.getRetry().getMaxAttempts());
        return attemptCount < maxAttempts;
    }

    private Duration computeDelay(long attemptCount) {
        long initial = Math.max(100L, backfillProperties.getRetry().getInitialDelayMs());
        double multiplier = Math.max(1.0d, backfillProperties.getRetry().getMultiplier());
        long maxDelay = Math.max(initial, backfillProperties.getRetry().getMaxDelayMs());

        double calculated = initial * Math.pow(multiplier, Math.max(0L, attemptCount));
        long delay = (long) Math.min(maxDelay, Math.max(initial, calculated));
        return Duration.ofMillis(delay);
    }
}
