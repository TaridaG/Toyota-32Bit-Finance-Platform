package com.company.marketdataservice.shared.resilience;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CircuitBreakerTest {

    @Test
    void opensAfterFailureThreshold() {
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofSeconds(30));
        breaker.recordFailure();
        assertDoesNotThrow(breaker::beforeCall);
        breaker.recordFailure();
        assertThrows(RuntimeException.class, breaker::beforeCall);
    }

    @Test
    void successResetsFailureCount() {
        CircuitBreaker breaker = new CircuitBreaker(2, Duration.ofSeconds(30));
        breaker.recordFailure();
        breaker.recordSuccess();
        breaker.recordFailure();
        assertDoesNotThrow(breaker::beforeCall);
    }
}
