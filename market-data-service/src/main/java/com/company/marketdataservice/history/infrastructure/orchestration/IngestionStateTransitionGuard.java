package com.company.marketdataservice.history.infrastructure.orchestration;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillStatus;
import com.company.marketdataservice.history.infrastructure.persistence.ChunkStatus;
import org.springframework.stereotype.Component;

/**
 * Backfill ve chunk state machine geçişlerinin geçerliliğini doğrular.
 */
@Component
public class IngestionStateTransitionGuard {

    /**
     * Geçiş veya invariant doğrulaması yapar.
         * @param from geçiş durumu
         * @param to geçiş durumu
         */
    public void assertStateTransitionAllowed(BackfillStatus from, BackfillStatus to) {
        if (from == null || to == null) {
            throw new IllegalStateException("Backfill transition requires non-null states");
        }
        boolean allowed = switch (from) {
            case NOT_STARTED -> to == BackfillStatus.RUNNING;
            case RUNNING -> to == BackfillStatus.COMPLETED || to == BackfillStatus.RETRYABLE || to == BackfillStatus.FAILED;
            case RETRYABLE -> to == BackfillStatus.RUNNING || to == BackfillStatus.FAILED;
            case FAILED -> to == BackfillStatus.RETRYABLE;
            case COMPLETED -> to == BackfillStatus.RUNNING;
        };
        if (!allowed) {
            throw new IllegalStateException("Invalid backfill transition: " + from + " -> " + to);
        }
    }

    /**
     * Geçiş veya invariant doğrulaması yapar.
         * @param from geçiş durumu
         * @param to geçiş durumu
         */
    public void assertChunkTransitionAllowed(ChunkStatus from, ChunkStatus to) {
        if (from == null || to == null) {
            throw new IllegalStateException("Chunk transition requires non-null states");
        }
        boolean allowed = switch (from) {
            case PENDING -> to == ChunkStatus.RUNNING;
            case RUNNING -> to == ChunkStatus.COMPLETED || to == ChunkStatus.RETRYABLE || to == ChunkStatus.FAILED;
            case RETRYABLE -> to == ChunkStatus.PENDING;
            case COMPLETED, FAILED -> false;
        };
        if (!allowed) {
            throw new IllegalStateException("Invalid chunk transition: " + from + " -> " + to);
        }
    }
}
