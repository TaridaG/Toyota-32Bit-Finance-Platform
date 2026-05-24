package com.company.marketdataservice.history.infrastructure.http.dto;
import java.time.Instant;

/**
 * `geçmiş veri ve backfill` REST API için HTTP DTO.
 */
public record IngestionStateItemDto(
        String assetType,
        String symbol,
        String status,
        Instant lastFetchedAt,
        Double progress,
        long chunksCompleted,
        long chunksRemaining,
        Long etaSeconds,
        String lastChunkWindow,
        String lockedBy,
        long attemptCount,
        Instant nextRetryAt,
        String errorCode,
        String errorMessage,
        String runId,
        Instant updatedAt
) {
}
