package com.company.logconsumer.shared.web;

import java.time.Instant;

/**
 * REST API hata gövdesi: kod, mesaj ve zaman damgası.
 */
public record ApiError(
        String code,
        String message,
        Instant timestamp
) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message, Instant.now());
    }
}
