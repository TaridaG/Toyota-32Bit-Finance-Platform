package com.company.marketdataservice.shared.resilience;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimiterTest {

    @Test
    void allowsUpToMaxRequestsPerSecond() {
        RateLimiter limiter = new RateLimiter(2);
        assertDoesNotThrow(limiter::acquire);
        assertDoesNotThrow(limiter::acquire);
    }

    @Test
    void rejectsWhenLimitExceeded() {
        RateLimiter limiter = new RateLimiter(1);
        limiter.acquire();
        assertThrows(RuntimeException.class, limiter::acquire);
    }
}
