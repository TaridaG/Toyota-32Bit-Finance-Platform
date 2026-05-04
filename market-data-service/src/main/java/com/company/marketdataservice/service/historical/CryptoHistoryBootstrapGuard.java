package com.company.marketdataservice.service.historical;

import com.company.marketdataservice.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.history.BackfillStateRepository;
import com.company.marketdataservice.history.BackfillStatus;
import com.company.marketdataservice.history.MarketPriceHistoryRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class CryptoHistoryBootstrapGuard {

    private static final Logger log = LoggerFactory.getLogger(CryptoHistoryBootstrapGuard.class);

    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final BackfillStateRepository backfillStateRepository;
    private final HistoricalBackfillService historicalBackfillService;
    private final MarketHistoryBackfillProperties backfillProperties;
    private final Set<String> readySymbols = ConcurrentHashMap.newKeySet();
    private final Set<String> bootstrapInProgress = ConcurrentHashMap.newKeySet();
    private final ExecutorService bootstrapExecutor = Executors.newSingleThreadExecutor();

    public CryptoHistoryBootstrapGuard(
            MarketPriceHistoryRepository marketPriceHistoryRepository,
            BackfillStateRepository backfillStateRepository,
            HistoricalBackfillService historicalBackfillService,
            MarketHistoryBackfillProperties backfillProperties
    ) {
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
        this.backfillStateRepository = backfillStateRepository;
        this.historicalBackfillService = historicalBackfillService;
        this.backfillProperties = backfillProperties;
    }

    public boolean isLiveAllowed(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            return false;
        }
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);

        if (readySymbols.contains(symbol)) {
            return true;
        }

        if (hasSufficientHistory(symbol)) {
            readySymbols.add(symbol);
            return true;
        }

        if (bootstrapInProgress.add(symbol)) {
            CompletableFuture.runAsync(() -> runBootstrap(symbol), bootstrapExecutor);
            log.info("CRYPTO_HISTORY_BOOTSTRAP_TRIGGERED symbol={}", symbol);
        }
        return false;
    }

    private void runBootstrap(String symbol) {
        try {
            resetBootstrapCheckpoint(symbol);
            historicalBackfillService.backfillPrice(symbol);
            if (hasSufficientHistory(symbol)) {
                readySymbols.add(symbol);
                log.info("CRYPTO_HISTORY_BOOTSTRAP_COMPLETED symbol={}", symbol);
            } else {
                log.warn("CRYPTO_HISTORY_BOOTSTRAP_NO_DATA symbol={}", symbol);
            }
        } catch (Exception ex) {
            log.warn("CRYPTO_HISTORY_BOOTSTRAP_FAILED symbol={} reason={}", symbol, ex.getMessage());
        } finally {
            bootstrapInProgress.remove(symbol);
        }
    }

    private void resetBootstrapCheckpoint(String symbol) {
        backfillStateRepository.findByAssetTypeAndSymbol("PRICE", symbol)
                .ifPresent(state -> {
                    state.setLastFetchedAt(null);
                    state.setStatus(BackfillStatus.NOT_STARTED.name());
                    state.setUpdatedAt(Instant.now());
                    backfillStateRepository.save(state);
                });
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
