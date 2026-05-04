package com.company.marketdataservice.history;

public enum BackfillStatus {
    NOT_STARTED,
    RUNNING,
    COMPLETED,
    RETRYABLE,
    FAILED;

    public static BackfillStatus fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return NOT_STARTED;
        }
        return BackfillStatus.valueOf(raw.trim().toUpperCase());
    }
}
