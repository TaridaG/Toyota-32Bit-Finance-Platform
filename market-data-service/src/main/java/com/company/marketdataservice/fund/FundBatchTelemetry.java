package com.company.marketdataservice.fund;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class FundBatchTelemetry {

    private final AtomicBoolean lastBatchUsedHttpFallback = new AtomicBoolean(false);

    public void markHttpFallback() {
        lastBatchUsedHttpFallback.set(true);
    }

    public boolean consumeHttpFallback() {
        return lastBatchUsedHttpFallback.getAndSet(false);
    }
}
