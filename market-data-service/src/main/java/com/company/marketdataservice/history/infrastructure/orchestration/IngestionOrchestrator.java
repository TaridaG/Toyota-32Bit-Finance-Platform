package com.company.marketdataservice.history.infrastructure.orchestration;
import com.company.marketdataservice.bootstrap.config.FundMarketProperties;
import com.company.marketdataservice.bootstrap.config.FxMarketProperties;
import com.company.marketdataservice.bootstrap.config.MarketDataProperties;
import com.company.marketdataservice.bootstrap.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillChunkEntry;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillChunkRepository;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillStateEntry;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillStateRepository;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Backfill ingestion job'larını leader election ile koordine eder.
 */
@Component
public class IngestionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(IngestionOrchestrator.class);
    private static final String TRY = "TRY";
    private static final int MAX_CHUNKS_PER_TASK_PER_RUN = 12;

    private final HistoricalBackfillService historicalBackfillService;
    private final IngestionStateService ingestionStateService;
    private final IngestionRetryPolicy retryPolicy;
    private final BackfillChunkRepository backfillChunkRepository;
    private final BackfillStateRepository backfillStateRepository;
    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final MarketHistoryBackfillProperties backfillProperties;
    private final MarketDataProperties marketDataProperties;
    private final FxMarketProperties fxMarketProperties;
    private final FundMarketProperties fundMarketProperties;

    public IngestionOrchestrator(
            HistoricalBackfillService historicalBackfillService,
            IngestionStateService ingestionStateService,
            IngestionRetryPolicy retryPolicy,
            BackfillChunkRepository backfillChunkRepository,
            BackfillStateRepository backfillStateRepository,
            MarketPriceHistoryRepository marketPriceHistoryRepository,
            MarketHistoryBackfillProperties backfillProperties,
            MarketDataProperties marketDataProperties,
            FxMarketProperties fxMarketProperties,
            FundMarketProperties fundMarketProperties
    ) {
        this.historicalBackfillService = historicalBackfillService;
        this.ingestionStateService = ingestionStateService;
        this.retryPolicy = retryPolicy;
        this.backfillChunkRepository = backfillChunkRepository;
        this.backfillStateRepository = backfillStateRepository;
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
        this.backfillProperties = backfillProperties;
        this.marketDataProperties = marketDataProperties;
        this.fxMarketProperties = fxMarketProperties;
        this.fundMarketProperties = fundMarketProperties;
    }

    /**
     * Katalog mapping reconciliation uygular.
         */
    public void reconcileAndRun() {
        if (!backfillProperties.isEnabled()) {
            log.info("Ingestion orchestrator skipped because market.history.backfill.enabled=false");
            return;
        }

        String runId = UUID.randomUUID().toString();
        List<IngestionTask> tasks = collectUniverse();
        for (IngestionTask task : tasks) {
            ingestionStateService.getOrCreate(task.assetType(), task.symbol());
        }

        List<IngestionTask> queued = queueDueTasks(runId);
        log.info("INGESTION_ORCHESTRATOR_START runId={} queuedJobs={} universeSize={}", runId, queued.size(), tasks.size());
        for (IngestionTask task : queued) {
            executeTask(task, runId);
        }
    }

    private List<IngestionTask> queueDueTasks(String runId) {
        Instant now = Instant.now();
        List<IngestionTask> queued = new ArrayList<>();
        for (BackfillStateEntry state : backfillStateRepository.findAll()) {
            if (isPriceHistorySufficient(state)) {
                if (BackfillStatus.fromValue(state.getStatus()) != BackfillStatus.COMPLETED) {
                    Instant safeLastFetched = state.getLastFetchedAt() == null ? Instant.now() : state.getLastFetchedAt();
                    ingestionStateService.markCompletedWithoutGuard(state, safeLastFetched);
                }
                continue;
            }
            if ("PRICE".equalsIgnoreCase(state.getAssetType())) {
                // If coverage is insufficient, stale completed chunks can pin checkpoint at "today"
                // and prevent historical rehydration. Clear chunk checkpoints before re-queueing.
                backfillChunkRepository.deleteByAssetTypeAndSymbol(state.getAssetType(), state.getSymbol());
            }
            BackfillStatus status = BackfillStatus.fromValue(state.getStatus());
            if (status == BackfillStatus.COMPLETED) {
                ingestionStateService.markNotStarted(state);
                queued.add(new IngestionTask(state.getAssetType(), state.getSymbol()));
                continue;
            }
            if (status == BackfillStatus.FAILED) {
                ingestionStateService.markNotStarted(state);
                queued.add(new IngestionTask(state.getAssetType(), state.getSymbol()));
                continue;
            }
            if (status == BackfillStatus.RUNNING) {
                ingestionStateService.markInterruptedAsRetryable(state, now, runId);
                queued.add(new IngestionTask(state.getAssetType(), state.getSymbol()));
                continue;
            }
            if (status == BackfillStatus.RETRYABLE) {
                if (state.getNextRetryAt() == null || !state.getNextRetryAt().isAfter(now)) {
                    queued.add(new IngestionTask(state.getAssetType(), state.getSymbol()));
                }
                continue;
            }
            queued.add(new IngestionTask(state.getAssetType(), state.getSymbol()));
        }
        return queued;
    }

    private void executeTask(IngestionTask task, String runId) {
        boolean runningMarked = false;
        int processedChunks = 0;
        while (true) {
            BackfillStateEntry state = ingestionStateService.getOrCreate(task.assetType(), task.symbol());
            HistoricalBackfillService.ChunkWindow window =
                    historicalBackfillService.nextChunkWindow(task.assetType(), task.symbol(), task.provider());
            if (window == null) {
                if (runningMarked) {
                    BackfillStateEntry refreshed = ingestionStateService.getOrCreate(task.assetType(), task.symbol());
                    ingestionStateService.markCompleted(refreshed, refreshed.getLastFetchedAt() == null ? Instant.now() : refreshed.getLastFetchedAt());
                }
                return;
            }
            if (!runningMarked) {
                ingestionStateService.markRunning(state, runId);
                runningMarked = true;
            }

            BackfillChunkEntry chunk = createPendingChunk(task, window);
            boolean claimed = backfillChunkRepository.tryMarkChunkRunning(chunk.getId(), runId);
            if (!claimed) {
                log.info("INGESTION_CHUNK_CLAIM_SKIPPED assetType={} symbol={} chunkId={}", task.assetType(), task.symbol(), chunk.getId());
                return;
            }

            try {
                String executionKey = chunkExecutionKey(task, window);
                historicalBackfillService.executeChunk(task.assetType(), task.symbol(), executionKey, window.start(), window.end());
                backfillChunkRepository.markChunkCompleted(chunk.getId());
                throttleChunkPace();
                processedChunks++;
                if (processedChunks >= MAX_CHUNKS_PER_TASK_PER_RUN) {
                    return;
                }
            } catch (Exception ex) {
                handleFailure(task, chunk.getId(), ex);
                return;
            }
        }
    }

    private BackfillChunkEntry createPendingChunk(IngestionTask task, HistoricalBackfillService.ChunkWindow window) {
        Instant windowStart = window.start().atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant windowEnd = window.end().atStartOfDay().toInstant(ZoneOffset.UTC);
        BackfillChunkEntry chunk = new BackfillChunkEntry();
        chunk.setAssetType(task.assetType());
        chunk.setSymbol(task.symbol());
        chunk.setProvider(task.provider());
        chunk.setWindowStart(windowStart);
        chunk.setWindowEnd(windowEnd);
        chunk.setStatus("PENDING");
        chunk.setAttemptCount(0L);
        Instant now = Instant.now();
        chunk.setCreatedAt(now);
        chunk.setUpdatedAt(now);
        try {
            return backfillChunkRepository.save(chunk);
        } catch (DataIntegrityViolationException ex) {
            return backfillChunkRepository.findByAssetTypeAndSymbolAndProviderAndWindowStartAndWindowEnd(
                            task.assetType(), task.symbol(), task.provider(), windowStart, windowEnd)
                    .orElseThrow(() -> ex);
        }
    }

    private void handleFailure(IngestionTask task, Long chunkId, Exception ex) {
        BackfillStateEntry current = ingestionStateService.getOrCreate(task.assetType(), task.symbol());
        String errorCode = ex.getClass().getSimpleName().toUpperCase(Locale.ROOT);
        if (isRetryable(ex) && retryPolicy.canRetry(current.getAttemptCount())) {
            Instant nextRetryAt = retryPolicy.nextRetryAt(current.getAttemptCount(), Instant.now());
            backfillChunkRepository.markChunkRetryable(chunkId, errorCode, ex.getMessage(), nextRetryAt);
            ingestionStateService.markRetryable(current, errorCode, ex.getMessage(), nextRetryAt);
            log.warn("INGESTION_RETRYABLE assetType={} symbol={} attemptCount={} nextRetryAt={} error={}",
                    task.assetType(), task.symbol(), current.getAttemptCount() + 1L, nextRetryAt, ex.getMessage());
            return;
        }
        backfillChunkRepository.markChunkFailed(chunkId, errorCode, ex.getMessage());
        ingestionStateService.markFailed(current, errorCode, ex.getMessage());
        log.warn("INGESTION_FAILED assetType={} symbol={} attemptCount={} error={}",
                task.assetType(), task.symbol(), current.getAttemptCount() + 1L, ex.getMessage());
    }

    private static boolean isRetryable(Exception ex) {
        return !(ex instanceof IllegalArgumentException);
    }

    private static String chunkExecutionKey(IngestionTask task, HistoricalBackfillService.ChunkWindow window) {
        return task.symbol() + "|" + task.provider() + "|" + window.end();
    }

    private void throttleChunkPace() {
        try {
            Thread.sleep(75L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isPriceHistorySufficient(BackfillStateEntry state) {
        if (!"PRICE".equalsIgnoreCase(state.getAssetType())) {
            return false;
        }
        long totalAvailableDays = marketPriceHistoryRepository.countDistinctDaysBySymbol(state.getSymbol());
        int minTotalDays = Math.max(30, backfillProperties.getMinPriceHistoryDays());
        if (totalAvailableDays < minTotalDays) {
            return false;
        }
        Instant recentFrom = Instant.now().minus(365, ChronoUnit.DAYS);
        long recentAvailableDays = marketPriceHistoryRepository.countDistinctDaysBySymbolSince(state.getSymbol(), recentFrom);
        int minRecentDays = Math.max(30, backfillProperties.getMinRecentPriceHistoryDays());
        if (recentAvailableDays < minRecentDays) {
            return false;
        }
        Instant recent30From = Instant.now().minus(30, ChronoUnit.DAYS);
        long recent30AvailableDays = marketPriceHistoryRepository.countDistinctDaysBySymbolSince(state.getSymbol(), recent30From);
        int minRecent30Days = Math.max(5, backfillProperties.getMinRecent30DayCoverageDays());
        return recent30AvailableDays >= minRecent30Days;
    }

    private List<IngestionTask> collectUniverse() {
        List<IngestionTask> tasks = new ArrayList<>();
        Set<String> priceSymbols = new LinkedHashSet<>();
        if (marketDataProperties.getTrackedSymbols() != null) {
            priceSymbols.addAll(marketDataProperties.getTrackedSymbols());
        }
        if (marketDataProperties.getTrackedStocks() != null) {
            priceSymbols.addAll(marketDataProperties.getTrackedStocks());
        }
        for (String symbol : priceSymbols) {
            if (symbol != null && !symbol.isBlank()) {
                tasks.add(new IngestionTask("PRICE", symbol.trim().toUpperCase(Locale.ROOT)));
            }
        }

        Set<String> fxSymbols = new LinkedHashSet<>();
        if (fxMarketProperties.getProviderCurrencies() != null) {
            for (String raw : fxMarketProperties.getProviderCurrencies()) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String base = raw.trim().toUpperCase(Locale.ROOT);
                if (!TRY.equals(base)) {
                    fxSymbols.add(base + TRY);
                }
            }
        }
        for (String symbol : fxSymbols) {
            tasks.add(new IngestionTask("FX", symbol, "COMPOSITE_FX"));
        }

        Set<String> funds = new LinkedHashSet<>();
        if (fundMarketProperties.getTrackedFundCodes() != null) {
            funds.addAll(fundMarketProperties.getTrackedFundCodes());
        }
        for (String code : funds) {
            if (code != null && !code.isBlank()) {
                tasks.add(new IngestionTask("FUND", code.trim().toUpperCase(Locale.ROOT)));
            }
        }
        return tasks;
    }

    private record IngestionTask(String assetType, String symbol, String provider) {
        IngestionTask(String assetType, String symbol) {
            this(assetType, symbol, "COMPOSITE");
        }
    }
}
