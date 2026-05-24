package com.company.marketdataservice.history.infrastructure.orchestration;

import com.company.marketdataservice.history.infrastructure.persistence.BackfillStatus;
import com.company.marketdataservice.history.infrastructure.persistence.ChunkStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IngestionStateTransitionGuardTest {

    private final IngestionStateTransitionGuard guard = new IngestionStateTransitionGuard();

    @Test
    void allowsNotStartedToRunning() {
        assertDoesNotThrow(() -> guard.assertStateTransitionAllowed(BackfillStatus.NOT_STARTED, BackfillStatus.RUNNING));
    }

    @Test
    void rejectsCompletedToFailed() {
        assertThrows(IllegalStateException.class,
                () -> guard.assertStateTransitionAllowed(BackfillStatus.COMPLETED, BackfillStatus.FAILED));
    }

    @Test
    void allowsRunningToRetryable() {
        assertDoesNotThrow(() -> guard.assertStateTransitionAllowed(BackfillStatus.RUNNING, BackfillStatus.RETRYABLE));
    }

    @Test
    void allowsPendingChunkToRunning() {
        assertDoesNotThrow(() -> guard.assertChunkTransitionAllowed(ChunkStatus.PENDING, ChunkStatus.RUNNING));
    }

    @Test
    void rejectsCompletedChunkTransition() {
        assertThrows(IllegalStateException.class,
                () -> guard.assertChunkTransitionAllowed(ChunkStatus.COMPLETED, ChunkStatus.RUNNING));
    }
}
