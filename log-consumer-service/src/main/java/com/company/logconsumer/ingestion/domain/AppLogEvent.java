package com.company.logconsumer.ingestion.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Kafka {@code app.logs} topic'inden gelen tek bir uygulama log satırının JSON modeli.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppLogEvent(
        String timestamp,
        String level,
        String serviceName,
        String message,
        String traceId,
        String spanId,
        String correlationId,
        String logger,
        String thread,
        String exception
) {}
