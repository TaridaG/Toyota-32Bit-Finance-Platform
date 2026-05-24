package com.company.gateway.shared.web;

import java.time.Instant;

/**
 * Gateway global error handler JSON response DTO'su.
 *
 * @param code sabit hata kodu (ör. {@code GATEWAY_ERROR})
 * @param message istemciye dönen kısa açıklama
 * @param timestamp hata anı UTC timestamp
 * @param correlationId trace için {@code X-Correlation-Id}
 */
public record ErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String correlationId
) {}