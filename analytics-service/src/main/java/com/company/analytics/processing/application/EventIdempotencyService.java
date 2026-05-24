package com.company.analytics.processing.application;

/** Kafka event'lerinin tekrar işlenmesini önleyen idempotency servis sözleşmesi. */
public interface EventIdempotencyService {

    /** Verilen event key daha önce işlendiyse true döner. */
    boolean isProcessed(String eventKey);

    /** Verilen event key'i işlenmiş olarak kalıcı olarak işaretler. */
    void markProcessed(String eventKey);

}