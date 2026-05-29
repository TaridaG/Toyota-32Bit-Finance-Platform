package com.company.marketdataservice.history.infrastructure.persistence;

import java.time.Instant;

public interface BackfillChunkRepositoryCustom {
    /**
     * Attempts to insert a chunk row idempotently.
     *
     * @return true if inserted, false if already existed
     */
    boolean insertIgnore(
            String assetType,
            String symbol,
            String provider,
            Instant windowStart,
            Instant windowEnd,
            String status,
            long attemptCount,
            Instant createdAt,
            Instant updatedAt
    );
}

