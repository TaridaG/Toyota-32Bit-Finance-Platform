package com.company.marketdataservice.history.infrastructure.orchestration;

import com.company.marketdataservice.bootstrap.config.MarketHistoryBackfillProperties;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IngestionRetryPolicyTest {

    @Test
    void canRetry_respectsMaxAttempts() {
        MarketHistoryBackfillProperties props = new MarketHistoryBackfillProperties();
        props.getRetry().setMaxAttempts(3);
        IngestionRetryPolicy policy = new IngestionRetryPolicy(props);

        assertTrue(policy.canRetry(0));
        assertTrue(policy.canRetry(2));
        assertFalse(policy.canRetry(3));
    }

    @Test
    void nextRetryAt_appliesExponentialBackoff() {
        MarketHistoryBackfillProperties props = new MarketHistoryBackfillProperties();
        props.getRetry().setInitialDelayMs(1_000L);
        props.getRetry().setMultiplier(2.0d);
        props.getRetry().setMaxDelayMs(10_000L);
        IngestionRetryPolicy policy = new IngestionRetryPolicy(props);

        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        Instant first = policy.nextRetryAt(0, now);
        Instant second = policy.nextRetryAt(1, now);

        assertTrue(first.isAfter(now));
        assertTrue(second.isAfter(first));
    }
}
