package com.company.marketdataservice.provider;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

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

    public synchronized void beforeCall() {

        if (state == State.OPEN) {
            if (Instant.now().isAfter(openTime.plus(openDuration))) {
                state = State.HALF_OPEN;
            } else {
                throw new RuntimeException("Circuit is OPEN");
            }
        }
    }

    public synchronized void recordSuccess() {
        failureCount.set(0);
        state = State.CLOSED;
    }

    public synchronized void recordFailure() {
        if (failureCount.incrementAndGet() >= failureThreshold) {
            state = State.OPEN;
            openTime = Instant.now();
        }
    }
}
