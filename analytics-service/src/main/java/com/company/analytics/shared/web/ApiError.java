package com.company.analytics.shared.web;

import java.time.LocalDateTime;

/** REST API hata yanıt DTO'su. */
public class ApiError {

    private String code;
    private String message;
    private LocalDateTime timestamp;

    /** Hata kodu ve mesajı ile ApiError oluşturur. */
    public ApiError(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    /** Hata kodunu döner. */
    public String getCode() {
        return code;
    }

    /** Hata mesajını döner. */
    public String getMessage() {
        return message;
    }

    /** Hatanın oluştuğu zaman damgasını döner. */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}