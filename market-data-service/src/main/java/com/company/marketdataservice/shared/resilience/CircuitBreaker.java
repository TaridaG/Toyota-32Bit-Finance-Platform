package com.company.marketdataservice.shared.resilience;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Basit in-memory circuit breaker; ardışık hatalarda provider çağrılarını keser.
 */
public class CircuitBreaker {

    private enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final int failureThreshold;
    private final Duration openDuration;

    private final AtomicInteger failureCount = new AtomicInteger(0);
    private volatile State state = State.CLOSED;
    private volatile Instant openTime;

    public CircuitBreaker(int failureThreshold, Duration openDuration) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
    }

    /**
     * Çağrı öncesi circuit breaker durumunu kontrol eder; OPEN ise exception fırlatır veya HALF_OPEN'a geçer.
     */
    public synchronized void beforeCall() {

        if (state == State.OPEN) {
            if (Instant.now().isAfter(openTime.plus(openDuration))) {
                state = State.HALF_OPEN;
            } else {
                throw new RuntimeException("Circuit is OPEN");
            }
        }
    }

    /**
     * Başarılı provider çağrısını kaydeder ve breaker'ı CLOSED durumuna döndürür.
     */
    public synchronized void recordSuccess() {
        failureCount.set(0);
        state = State.CLOSED;
    }

    /**
     * Başarısız çağrıyı sayar; eşik aşılırsa breaker'ı OPEN yapar.
     */
    public synchronized void recordFailure() {
        if (failureCount.incrementAndGet() >= failureThreshold) {
            state = State.OPEN;
            openTime = Instant.now();
        }
    }
}
