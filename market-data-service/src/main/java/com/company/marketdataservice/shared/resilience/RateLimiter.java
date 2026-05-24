package com.company.marketdataservice.shared.resilience;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Saniye başına istek limiti uygulayan basit rate limiter.
 */
public class RateLimiter {

    private final int maxRequestsPerSecond;
    private final AtomicInteger counter = new AtomicInteger(0);
    private volatile long currentSecond = Instant.now().getEpochSecond();

    public RateLimiter(int maxRequestsPerSecond) {
        this.maxRequestsPerSecond = maxRequestsPerSecond;
    }

    /**
     * Rate limit token'ı acquire eder; saniye kotası aşılırsa exception fırlatır.
     */
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
