package com.company.marketdataservice.history.infrastructure.orchestration;

import com.company.marketdataservice.bootstrap.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.catalog.application.InstrumentIngestScopeService;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Triggers BIST/NASDAQ historical backfill on startup. Live publish is gated only when configured.
 */
@Component
public class StockHistoryBootstrapGuard {

    private static final Logger log = LoggerFactory.getLogger(StockHistoryBootstrapGuard.class);

    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final HistoricalBackfillService historicalBackfillService;
    private final MarketHistoryBackfillProperties backfillProperties;
    private final InstrumentIngestScopeService ingestScope;
    private final Set<String> readySymbols = ConcurrentHashMap.newKeySet();
    private final Set<String> bootstrapInProgress = ConcurrentHashMap.newKeySet();
    private final ExecutorService bootstrapExecutor;

    public StockHistoryBootstrapGuard(
            MarketPriceHistoryRepository marketPriceHistoryRepository,
            HistoricalBackfillService historicalBackfillService,
            MarketHistoryBackfillProperties backfillProperties,
            InstrumentIngestScopeService ingestScope
    ) {
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
        this.historicalBackfillService = historicalBackfillService;
        this.backfillProperties = backfillProperties;
        this.ingestScope = ingestScope;
        int parallelism = Math.max(1, backfillProperties.getStockBootstrapParallelism());
        this.bootstrapExecutor = Executors.newFixedThreadPool(parallelism);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(150)
    public void warmUpOnStartup() {
        if (!backfillProperties.isEnabled()) {
            return;
        }
        for (IngestInstrumentDef def : ingestScope.resolvePolledEquityDefinitions()) {
            if (def != null && def.symbol() != null && !def.symbol().isBlank()) {
                triggerBackgroundBootstrap(def.symbol());
            }
        }
    }

    public boolean isLiveAllowed(String rawSymbol) {
        if (!backfillProperties.isEnabled()) {
            return true;
        }
        if (rawSymbol == null || rawSymbol.isBlank()) {
            return false;
        }
        triggerBackgroundBootstrap(rawSymbol);
        if (!backfillProperties.isGateLiveUntilHistoryReady()) {
            return true;
        }
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        if (readySymbols.contains(symbol)) {
            return true;
        }
        if (hasSufficientHistory(symbol)) {
            readySymbols.add(symbol);
            return true;
        }
        return false;
    }

    private void triggerBackgroundBootstrap(String rawSymbol) {
        if (!backfillProperties.isEnabled()) {
            return;
        }
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        if (readySymbols.contains(symbol) || bootstrapInProgress.contains(symbol)) {
            return;
        }
        if (hasSufficientHistory(symbol)) {
            readySymbols.add(symbol);
            return;
        }
        if (bootstrapInProgress.add(symbol)) {
            CompletableFuture.runAsync(() -> runBootstrap(symbol), bootstrapExecutor);
            log.info("STOCK_HISTORY_BOOTSTRAP_TRIGGERED symbol={}", symbol);
        }
    }

    private void runBootstrap(String symbol) {
        try {
            historicalBackfillService.backfillPrice(symbol);
            if (hasSufficientHistory(symbol)) {
                readySymbols.add(symbol);
                log.info("STOCK_HISTORY_BOOTSTRAP_COMPLETED symbol={}", symbol);
            } else {
                log.info(
                        "STOCK_HISTORY_BOOTSTRAP_PARTIAL symbol={} liveGate={}",
                        symbol,
                        backfillProperties.isGateLiveUntilHistoryReady()
                );
            }
        } catch (Exception ex) {
            log.warn("STOCK_HISTORY_BOOTSTRAP_FAILED symbol={} reason={}", symbol, ex.getMessage());
        } finally {
            bootstrapInProgress.remove(symbol);
        }
    }

    private boolean hasSufficientHistory(String symbol) {
        long totalDays = marketPriceHistoryRepository.countDistinctDaysBySymbol(symbol);
        int minTotalDays = Math.max(30, backfillProperties.getMinPriceHistoryDays());
        if (totalDays < minTotalDays) {
            return false;
        }
        Instant recentFrom = Instant.now().minus(365, ChronoUnit.DAYS);
        long recentDays = marketPriceHistoryRepository.countDistinctDaysBySymbolSince(symbol, recentFrom);
        int minRecentDays = Math.max(30, backfillProperties.getMinRecentPriceHistoryDays());
        return recentDays >= minRecentDays;
    }

    @PreDestroy
    void shutdownExecutor() {
        bootstrapExecutor.shutdownNow();
    }
}
