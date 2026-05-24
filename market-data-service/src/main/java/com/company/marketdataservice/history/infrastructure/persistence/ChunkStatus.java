package com.company.marketdataservice.history.infrastructure.persistence;
/**
 * `geçmiş veri ve backfill` infrastructure katmanı adaptörü.
 */
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
