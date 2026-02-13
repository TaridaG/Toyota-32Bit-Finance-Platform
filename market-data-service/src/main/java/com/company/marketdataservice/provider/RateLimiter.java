package com.company.marketdataservice.provider;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {

    private final int maxRequestsPerSecond;
    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile long currentSecond = Instant.now().getEpochSecond();

    public RateLimiter(int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    public synchronized void acquire() {

        long now = Instant.now().getEpochSecond();

        if (now != currentSecond) {
            currentSecond = now;
            counter.set(0);
        }

        if (counter.incrementAndGet() > maxRequestsPerSecond) {
            throw new RuntimeException("Rate limit exceeded");
        }
    }
}
