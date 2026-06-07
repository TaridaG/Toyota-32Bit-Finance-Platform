package com.company.marketdataservice.history.infrastructure.orchestration;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillStateEntry;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillStateRepository;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * `geçmiş veri ve backfill` infrastructure katmanı adaptörü.
 */
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

    /**
     * Veriyi okur ve döner.
         * @param assetType varlık türü (ör. STOCK, FX)
         * @param symbol enstrüman sembolü
         * @return mevcut veya yeni oluşturulan backfill state kaydı
         */
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

    /**
     * Backfill state'i {@link BackfillStatus#RUNNING} durumuna geçirir.
         * @param state güncellenecek backfill state kaydı
         * @param runId aktif orchestrator run kimliği
         */
    @Transactional
    public void markRunning(BackfillStateEntry state, String runId) {
        transitionGuard.assertStateTransitionAllowed(BackfillStatus.fromValue(state.getStatus()), BackfillStatus.RUNNING);
        state.setStatus(BackfillStatus.RUNNING.name());
        state.setRunId(runId);
        state.setUpdatedAt(Instant.now());
        backfillStateRepository.save(state);
    }

    /**
     * Backfill state'i {@link BackfillStatus#COMPLETED} durumuna geçirir ve son fetch zamanını kaydeder.
         * @param state güncellenecek backfill state kaydı
         * @param lastFetchedAt tamamlanan chunk'un son gözlem anı
         */
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

    /**
     * Geçici hatayı {@link BackfillStatus#RETRYABLE} durumuna işler ve bir sonraki retry zamanını planlar.
         * @param state güncellenecek backfill state kaydı
         * @param errorCode kısa hata kodu
         * @param errorMessage hata açıklaması
         * @param nextRetryAt bir sonraki retry denemesi zamanı
         */
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

    /**
     * Kalıcı hatayı {@link BackfillStatus#FAILED} durumuna işler.
         * @param state güncellenecek backfill state kaydı
         * @param errorCode kısa hata kodu
         * @param errorMessage hata açıklaması
         */
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

    /**
     * Yarım kalan run'ı {@link BackfillStatus#RETRYABLE} durumuna alır (INTERRUPTED).
         * @param state güncellenecek backfill state kaydı
         * @param nextRetryAt bir sonraki retry denemesi zamanı
         * @param runId yeni orchestrator run kimliği
         */
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

    /**
     * Backfill state'i {@link BackfillStatus#NOT_STARTED} durumuna sıfırlar.
         * @param state sıfırlanacak backfill state kaydı
         */
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

    /**
     * Transition guard olmadan state'i {@link BackfillStatus#COMPLETED} olarak işaretler (bootstrap senaryoları).
         * @param state güncellenecek backfill state kaydı
         * @param lastFetchedAt tamamlanan chunk'un son gözlem anı
         */
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
