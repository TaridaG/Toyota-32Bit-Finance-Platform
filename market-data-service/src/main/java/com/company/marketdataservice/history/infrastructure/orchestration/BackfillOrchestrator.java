package com.company.marketdataservice.history.infrastructure.orchestration;
import com.company.marketdataservice.bootstrap.config.MarketHistoryBackfillProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * `geçmiş veri ve backfill` backfill/ingestion orchestration bileşeni.
 */
@Component
public class BackfillOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(BackfillOrchestrator.class);

    private final IngestionOrchestrator ingestionOrchestrator;
    private final MarketHistoryBackfillProperties backfillProperties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public BackfillOrchestrator(
            IngestionOrchestrator ingestionOrchestrator,
            MarketHistoryBackfillProperties backfillProperties
    ) {
        this.ingestionOrchestrator = ingestionOrchestrator;
        this.backfillProperties = backfillProperties;
    }

    /**
     * İşi çalıştırır.
         */
    @EventListener(ApplicationReadyEvent.class)
    public void runOnStartup() {
        if (!backfillProperties.isEnabled() || !backfillProperties.isRunOnStartup()) {
            log.info("Historical backfill is disabled");
            return;
        }

        CompletableFuture.runAsync(() -> {
            BackfillExecutionContext.activateBootstrap();
            try {
                runBackfillSafely();
            } finally {
                BackfillExecutionContext.clear();
            }
        });
    }

    /**
     * İşi çalıştırır.
         */
    @Scheduled(
            initialDelayString = "${market.history.backfill.schedule-initial-delay-ms:30000}",
            fixedDelayString = "${market.history.backfill.schedule-delay-ms:900000}"
    )
    public void runPeriodic() {
        if (!backfillProperties.isEnabled()) {
            return;
        }
        CompletableFuture.runAsync(this::runBackfillSafely);
    }

    private void runBackfillSafely() {
        if (!running.compareAndSet(false, true)) {
            log.info("BACKFILL_ORCHESTRATOR_SKIPPED reason=already_running");
            return;
        }
        try {
            ingestionOrchestrator.reconcileAndRun();
        } catch (Exception ex) {
            log.warn("BACKFILL_ORCHESTRATOR_FAILED reason={}", ex.getMessage());
        } finally {
            running.set(false);
        }
    }
}
