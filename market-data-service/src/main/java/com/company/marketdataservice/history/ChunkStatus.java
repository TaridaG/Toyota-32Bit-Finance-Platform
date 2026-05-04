package com.company.marketdataservice.history;

public enum ChunkStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    RETRYABLE,
    FAILED;

    public static ChunkStatus fromValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return PENDING;
        }
        return ChunkStatus.valueOf(raw.trim().toUpperCase());
    }
}
