package com.company.marketdataservice.history;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BackfillChunkRepository extends JpaRepository<BackfillChunkEntry, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE mds_backfill_chunk
            SET status = 'RUNNING',
                attempt_count = CASE WHEN status = 'RETRYABLE' THEN attempt_count + 1 ELSE attempt_count END,
                started_at = now(),
                updated_at = now()
            WHERE id = :chunkId
              AND (
                    status = 'PENDING'
                    OR (
                        status = 'RETRYABLE'
                        AND (next_retry_at IS NULL OR next_retry_at <= now())
                    )
                  )
            """, nativeQuery = true)
    int tryMarkChunkRunningRows(@Param("chunkId") Long chunkId);

    default boolean tryMarkChunkRunning(Long chunkId, String instanceId) {
        return tryMarkChunkRunningRows(chunkId) == 1;
    }

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE mds_backfill_chunk
            SET status = 'COMPLETED',
                completed_at = now(),
                error_code = NULL,
                error_message = NULL,
                next_retry_at = NULL,
                updated_at = now()
            WHERE id = :chunkId
            """, nativeQuery = true)
    int markChunkCompleted(@Param("chunkId") Long chunkId);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE mds_backfill_chunk
            SET status = 'RETRYABLE',
                attempt_count = attempt_count + 1,
                error_code = :errorCode,
                error_message = :errorMessage,
                next_retry_at = :nextRetryAt,
                updated_at = now()
            WHERE id = :chunkId
            """, nativeQuery = true)
    int markChunkRetryable(
            @Param("chunkId") Long chunkId,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("nextRetryAt") Instant nextRetryAt
    );

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE mds_backfill_chunk
            SET status = 'FAILED',
                attempt_count = attempt_count + 1,
                error_code = :errorCode,
                error_message = :errorMessage,
                next_retry_at = NULL,
                updated_at = now()
            WHERE id = :chunkId
            """, nativeQuery = true)
    int markChunkFailed(
            @Param("chunkId") Long chunkId,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage
    );

    long countByAssetTypeAndSymbolAndStatusIn(String assetType, String symbol, Collection<String> statuses);

    List<BackfillChunkEntry> findTop1ByAssetTypeAndSymbolOrderByUpdatedAtDesc(String assetType, String symbol);

    Optional<BackfillChunkEntry> findTop1ByAssetTypeAndSymbolAndProviderAndStatusOrderByWindowEndDesc(
            String assetType,
            String symbol,
            String provider,
            String status
    );

    Optional<BackfillChunkEntry> findByAssetTypeAndSymbolAndProviderAndWindowStartAndWindowEnd(
            String assetType,
            String symbol,
            String provider,
            Instant windowStart,
            Instant windowEnd
    );

    @Query(value = """
            SELECT MIN(created_at), MAX(completed_at)
            FROM mds_backfill_chunk
            WHERE asset_type = :assetType
              AND symbol = :symbol
              AND status = 'COMPLETED'
            """, nativeQuery = true)
    Object[] getCompletedWindowBounds(@Param("assetType") String assetType, @Param("symbol") String symbol);
}
