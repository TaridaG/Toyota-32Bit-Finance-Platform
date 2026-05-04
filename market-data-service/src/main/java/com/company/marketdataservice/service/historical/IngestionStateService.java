package com.company.marketdataservice.service.historical;

import com.company.marketdataservice.history.BackfillStateEntry;
import com.company.marketdataservice.history.BackfillStateRepository;
import com.company.marketdataservice.history.BackfillStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class IngestionStateService {

    private final BackfillStateRepository backfillStateRepository;
    private final IngestionStateTransitionGuard transitionGuard;

    public IngestionStateService(
            BackfillStateRepository backfillStateRepository,
            IngestionStateTransitionGuard transitionGuard
    ) {
        this.backfillStateRepository = backfillStateRepository;
        this.transitionGuard = transitionGuard;
    }

    @Transactional
    public BackfillStateEntry getOrCreate(String assetType, String symbol) {
        return backfillStateRepository.findByAssetTypeAndSymbol(assetType, symbol)
                .orElseGet(() -> {
                    BackfillStateEntry created = new BackfillStateEntry();
                    created.setAssetType(assetType);
                    created.setSymbol(symbol);
                    created.setStatus(BackfillStatus.NOT_STARTED.name());
                    created.setAttemptCount(0L);
                    created.setUpdatedAt(Instant.now());
                    return backfillStateRepository.save(created);
                });
    }

    @Transactional
    public void markRunning(BackfillStateEntry state, String runId) {
        transitionGuard.assertStateTransitionAllowed(BackfillStatus.fromValue(state.getStatus()), BackfillStatus.RUNNING);
        state.setStatus(BackfillStatus.RUNNING.name());
        state.setRunId(runId);
        state.setUpdatedAt(Instant.now());
        backfillStateRepository.save(state);
    }

    @Transactional
    public void markCompleted(BackfillStateEntry state, Instant lastFetchedAt) {
        transitionGuard.assertStateTransitionAllowed(BackfillStatus.fromValue(state.getStatus()), BackfillStatus.COMPLETED);
        state.setStatus(BackfillStatus.COMPLETED.name());
        state.setLastFetchedAt(lastFetchedAt);
        state.setErrorCode(null);
        state.setErrorMessage(null);
        state.setNextRetryAt(null);
        state.setUpdatedAt(Instant.now());
        backfillStateRepository.save(state);
    }

    @Transactional
    public void markRetryable(BackfillStateEntry state, String errorCode, String errorMessage, Instant nextRetryAt) {
        transitionGuard.assertStateTransitionAllowed(BackfillStatus.fromValue(state.getStatus()), BackfillStatus.RETRYABLE);
        state.setStatus(BackfillStatus.RETRYABLE.name());
        state.setAttemptCount(state.getAttemptCount() + 1L);
        state.setErrorCode(errorCode);
        state.setErrorMessage(truncate(errorMessage));
        state.setNextRetryAt(nextRetryAt);
        state.setUpdatedAt(Instant.now());
        backfillStateRepository.save(state);
    }

    @Transactional
    public void markFailed(BackfillStateEntry state, String errorCode, String errorMessage) {
        transitionGuard.assertStateTransitionAllowed(BackfillStatus.fromValue(state.getStatus()), BackfillStatus.FAILED);
        state.setStatus(BackfillStatus.FAILED.name());
        state.setAttemptCount(state.getAttemptCount() + 1L);
        state.setErrorCode(errorCode);
        state.setErrorMessage(truncate(errorMessage));
        state.setNextRetryAt(null);
        state.setUpdatedAt(Instant.now());
        backfillStateRepository.save(state);
    }

    @Transactional
    public void markInterruptedAsRetryable(BackfillStateEntry state, Instant nextRetryAt, String runId) {
        transitionGuard.assertStateTransitionAllowed(BackfillStatus.fromValue(state.getStatus()), BackfillStatus.RETRYABLE);
        state.setStatus(BackfillStatus.RETRYABLE.name());
        state.setRunId(runId);
        state.setErrorCode("INTERRUPTED");
        state.setErrorMessage("Previous run interrupted before completion.");
        state.setNextRetryAt(nextRetryAt);
        state.setUpdatedAt(Instant.now());
        backfillStateRepository.save(state);
    }

    @Transactional
    public void markNotStarted(BackfillStateEntry state) {
        state.setStatus(BackfillStatus.NOT_STARTED.name());
        state.setLastFetchedAt(null);
        state.setAttemptCount(0L);
        state.setErrorCode(null);
        state.setErrorMessage(null);
        state.setNextRetryAt(null);
        state.setRunId(null);
        state.setUpdatedAt(Instant.now());
        backfillStateRepository.save(state);
    }

    @Transactional
    public void markCompletedWithoutGuard(BackfillStateEntry state, Instant lastFetchedAt) {
        state.setStatus(BackfillStatus.COMPLETED.name());
        state.setLastFetchedAt(lastFetchedAt);
        state.setErrorCode(null);
        state.setErrorMessage(null);
        state.setNextRetryAt(null);
        state.setRunId(null);
        state.setUpdatedAt(Instant.now());
        backfillStateRepository.save(state);
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 4000 ? message.substring(0, 4000) : message;
    }
}
