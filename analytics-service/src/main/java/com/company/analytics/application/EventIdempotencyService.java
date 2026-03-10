package com.company.analytics.application;

public interface EventIdempotencyService {

    boolean isProcessed(String eventKey);

    void markProcessed(String eventKey);

}