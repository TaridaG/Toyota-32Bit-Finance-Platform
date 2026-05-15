package com.company.marketdataservice.service.historical;

import com.company.marketdataservice.event.FundSnapshotUpdatedEvent;
import com.company.marketdataservice.event.FxSnapshotUpdatedEvent;
import com.company.marketdataservice.event.MarketPriceUpdatedEvent;
import com.company.marketdataservice.config.FundMarketProperties;
import com.company.marketdataservice.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.historical.HistoricalFundPoint;
import com.company.marketdataservice.historical.HistoricalFundProvider;
import com.company.marketdataservice.historical.HistoricalFxPoint;
import com.company.marketdataservice.historical.HistoricalFxProvider;
import com.company.marketdataservice.historical.HistoricalPricePoint;
import com.company.marketdataservice.historical.HistoricalPriceProvider;
import com.company.marketdataservice.history.BackfillChunkRepository;
import com.company.marketdataservice.history.BackfillStateEntry;
import com.company.marketdataservice.history.BackfillStateRepository;
import com.company.marketdataservice.history.BackfillStatus;
import com.company.marketdataservice.service.history.FundHistoryWriteService;
import com.company.marketdataservice.service.history.FxHistoryWriteService;
import com.company.marketdataservice.service.history.MarketHistoryWriteService;
import com.company.marketdataservice.provider.binance.BinanceHistoricalPriceProvider;
import com.company.marketdataservice.provider.finnhub.FinnhubHistoricalPriceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Service
public class HistoricalBackfillService {

    private static final int SAFE_DEFAULT_BACKFILL_YEARS = 5;
    private static final int SAFE_DEFAULT_CHUNK_DAYS = 30;
    private static final Logger log = LoggerFactory.getLogger(HistoricalBackfillService.class);

    private final BackfillStateRepository backfillStateRepository;
    private final BackfillChunkRepository backfillChunkRepository;
    private final MarketHistoryWriteService marketHistoryWriteService;
    private final FxHistoryWriteService fxHistoryWriteService;
    private final FundHistoryWriteService fundHistoryWriteService;
    private final MarketHistoryBackfillProperties backfillProperties;
    private final FundMarketProperties fundMarketProperties;
    private final List<HistoricalPriceProvider> historicalPriceProviders;
    private final List<HistoricalFxProvider> historicalFxProviders;
    private final List<HistoricalFundProvider> historicalFundProviders;
    private boolean noPriceProvidersLogged;
    private boolean noFxProvidersLogged;
    private boolean noFundProvidersLogged;

    public HistoricalBackfillService(
            BackfillStateRepository backfillStateRepository,
            BackfillChunkRepository backfillChunkRepository,
            MarketHistoryWriteService marketHistoryWriteService,
            FxHistoryWriteService fxHistoryWriteService,
            FundHistoryWriteService fundHistoryWriteService,
            MarketHistoryBackfillProperties backfillProperties,
            FundMarketProperties fundMarketProperties,
            List<HistoricalPriceProvider> historicalPriceProviders,
            List<HistoricalFxProvider> historicalFxProviders,
            List<HistoricalFundProvider> historicalFundProviders
    ) {
        this.backfillStateRepository = backfillStateRepository;
        this.backfillChunkRepository = backfillChunkRepository;
        this.marketHistoryWriteService = marketHistoryWriteService;
        this.fxHistoryWriteService = fxHistoryWriteService;
        this.fundHistoryWriteService = fundHistoryWriteService;
        this.backfillProperties = backfillProperties;
        this.fundMarketProperties = fundMarketProperties;
        this.historicalPriceProviders = historicalPriceProviders;
        this.historicalFxProviders = historicalFxProviders;
        this.historicalFundProviders = historicalFundProviders;
    }

    public void backfillPrice(String symbol) {
        if (historicalPriceProviders.isEmpty()) {
            logNoProvidersOnce("PRICE");
            return;
        }
        backfill("PRICE", symbol, (chunkStart, chunkEnd) -> {
            for (HistoricalPriceProvider provider : historicalPriceProviders) {
                List<HistoricalPricePoint> points = provider.fetchRange(symbol, chunkStart, chunkEnd);
                List<MarketPriceUpdatedEvent> events = new ArrayList<>();
                for (HistoricalPricePoint p : points) {
                    events.add(new MarketPriceUpdatedEvent(
                            UUID.randomUUID().toString(),
                            p.symbol(),
                            p.price(),
                            p.priceType() == null ? "MARKET" : p.priceType(),
                            p.source() == null ? "HISTORICAL" : p.source(),
                            p.occurredAt() == null ? Instant.now() : p.occurredAt(),
                            p.instrumentId()
                    ));
                }
                marketHistoryWriteService.saveBatch(events);
            }
        });
    }

    public void backfillFx(String canonicalSymbol) {
        if (historicalFxProviders.isEmpty()) {
            logNoProvidersOnce("FX");
            return;
        }
        backfill("FX", canonicalSymbol, (chunkStart, chunkEnd) -> {
            for (HistoricalFxProvider provider : historicalFxProviders) {
                List<HistoricalFxPoint> points = provider.fetchRange(canonicalSymbol, chunkStart, chunkEnd);
                List<FxSnapshotUpdatedEvent> events = new ArrayList<>();
                for (HistoricalFxPoint p : points) {
                    events.add(new FxSnapshotUpdatedEvent(
                            UUID.randomUUID().toString(),
                            p.canonicalSymbol(),
                            p.instrumentId(),
                            p.baseCurrency(),
                            p.quoteCurrency(),
                            p.bid(),
                            p.ask(),
                            p.mid(),
                            p.occurredAt(),
                            p.source()
                    ));
                }
                fxHistoryWriteService.saveBatch(events);
            }
        });
    }

    public void backfillFund(String fundCode) {
        if (historicalFundProviders.isEmpty()) {
            logNoProvidersOnce("FUND");
            return;
        }
        backfill("FUND", fundCode, (chunkStart, chunkEnd) -> {
            for (HistoricalFundProvider provider : historicalFundProviders) {
                List<HistoricalFundPoint> points = provider.fetchRange(fundCode, chunkStart, chunkEnd);
                List<FundSnapshotUpdatedEvent> events = new ArrayList<>();
                for (HistoricalFundPoint p : points) {
                    events.add(new FundSnapshotUpdatedEvent(
                            UUID.randomUUID().toString(),
                            p.fundCode(),
                            p.instrumentId(),
                            p.nav(),
                            p.occurredAt(),
                            p.source()
                    ));
                }
                fundHistoryWriteService.saveBatch(events);
            }
        });
    }

    public ChunkWindow nextChunkWindow(String assetType, String rawSymbol, String provider) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            return null;
        }
        if (!hasProviders(assetType)) {
            logNoProvidersOnce(assetType);
            return null;
        }
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        BackfillStateEntry state = getOrCreate(assetType, symbol);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = resolveStartDate(assetType, symbol, provider, state, today);
        if (startDate.isAfter(today)) {
            return null;
        }
        int chunkDays = effectiveChunkDays();
        LocalDate chunkEnd = startDate.plusDays(chunkDays - 1L);
        if (chunkEnd.isAfter(today)) {
            chunkEnd = today;
        }
        return new ChunkWindow(startDate, chunkEnd);
    }

    public void executeChunk(String assetType, String rawSymbol, String chunkExecutionKey, LocalDate chunkStart, LocalDate chunkEnd) {
        if (rawSymbol == null || rawSymbol.isBlank() || chunkStart == null || chunkEnd == null) {
            return;
        }
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        BackfillStateEntry state = getOrCreate(assetType, symbol);
        log.info("BACKFILL_CHUNK_EXECUTE asset={} symbol={} from={} to={}", assetType, symbol, chunkStart, chunkEnd);
        try {
            BackfillExecutionContext.activate();
            switch (assetType) {
                case "PRICE" -> executePriceChunk(symbol, chunkExecutionKey, chunkStart, chunkEnd);
                case "FX" -> executeFxChunk(symbol, chunkExecutionKey, chunkStart, chunkEnd);
                case "FUND" -> executeFundChunk(symbol, chunkExecutionKey, chunkStart, chunkEnd);
                default -> throw new IllegalArgumentException("Unknown assetType: " + assetType);
            }
            markChunkDone(state, chunkEnd);
            log.info("BACKFILL_CHUNK_DONE asset={} symbol={} lastFetched={}", assetType, symbol, state.getLastFetchedAt());
        } finally {
            BackfillExecutionContext.clear();
            sleepBetweenChunks();
        }
    }

    private void backfill(String assetType, String rawSymbol, ChunkProcessor processor) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            return;
        }
        String symbol = rawSymbol.trim().toUpperCase(Locale.ROOT);
        BackfillStateEntry state = getOrCreate(assetType, symbol);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = resolveStartDate(state, today);
        if (startDate.isAfter(today)) {
            return;
        }

        log.info("BACKFILL_START asset={} symbol={} start={} end={}", assetType, symbol, startDate, today);

        int chunkDays = effectiveChunkDays();
        LocalDate chunkStart = startDate;
        while (!chunkStart.isAfter(today)) {
            LocalDate chunkEnd = chunkStart.plusDays(chunkDays - 1L);
            if (chunkEnd.isAfter(today)) {
                chunkEnd = today;
            }

            log.info("BACKFILL_CHUNK asset={} symbol={} from={} to={}", assetType, symbol, chunkStart, chunkEnd);
            try {
                BackfillExecutionContext.activate();
                processor.process(chunkStart, chunkEnd);
                markChunkDone(state, chunkEnd);
                log.info("BACKFILL_PROGRESS symbol={} lastFetched={}", symbol, state.getLastFetchedAt());
            } catch (Exception ex) {
                log.warn(
                        "BACKFILL_ERROR asset={} symbol={} from={} to={} reason={}",
                        assetType,
                        symbol,
                        chunkStart,
                        chunkEnd,
                        ex.getMessage()
                );
                throw new IllegalStateException("Backfill failed for " + assetType + ":" + symbol, ex);
            } finally {
                BackfillExecutionContext.clear();
                sleepBetweenChunks();
            }

            chunkStart = chunkEnd.plusDays(1L);
        }

        log.info("BACKFILL_COMPLETE asset={} symbol={} lastFetched={}", assetType, symbol, state.getLastFetchedAt());
    }

    private LocalDate resolveStartDate(String assetType, String symbol, String provider, BackfillStateEntry state, LocalDate today) {
        Instant checkpoint = backfillChunkRepository
                .findTop1ByAssetTypeAndSymbolAndProviderAndStatusOrderByWindowEndDesc(
                        assetType,
                        symbol,
                        provider == null || provider.isBlank() ? "COMPOSITE" : provider,
                        "COMPLETED"
                )
                .map(chunk -> chunk.getWindowEnd() == null ? null : chunk.getWindowEnd().plus(1, ChronoUnit.MILLIS))
                .orElse(state.getLastFetchedAt());
        if (checkpoint == null) {
            int years =
                    "FUND".equalsIgnoreCase(assetType) ? effectiveFundBackfillYears() : effectiveBackfillYears();
            return today.minusYears(years);
        }
        return checkpoint.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private LocalDate resolveStartDate(BackfillStateEntry state, LocalDate today) {
        if (state.getLastFetchedAt() == null) {
            return today.minusYears(effectiveBackfillYears());
        }
        return state.getLastFetchedAt().atZone(ZoneOffset.UTC).toLocalDate().plusDays(1L);
    }

    private int effectiveBackfillYears() {
        int years = backfillProperties.getYears();
        if (years <= 0) {
            log.warn(
                    "Invalid backfill years configured: {}. Falling back to safe default {}.",
                    years,
                    SAFE_DEFAULT_BACKFILL_YEARS
            );
            return SAFE_DEFAULT_BACKFILL_YEARS;
        }
        if (years > 10) {
            log.warn("Backfill years {} exceeds cap 10. Using capped value.", years);
            return 10;
        }
        return years;
    }

    private int effectiveFundBackfillYears() {
        int years = fundMarketProperties.getHistoricalNavBackfillYears();
        if (years <= 0) {
            return 1;
        }
        return Math.min(years, 10);
    }

    private int effectiveChunkDays() {
        int chunkDays = backfillProperties.getChunkDays();
        if (chunkDays <= 0) {
            log.warn(
                    "Invalid backfill chunk-days configured: {}. Falling back to safe default {}.",
                    chunkDays,
                    SAFE_DEFAULT_CHUNK_DAYS
            );
            return SAFE_DEFAULT_CHUNK_DAYS;
        }
        if (chunkDays > 90) {
            log.warn("Backfill chunk-days {} exceeds cap 90. Using capped value.", chunkDays);
            return 90;
        }
        return chunkDays;
    }

    private void sleepBetweenChunks() {
        long sleepMs = backfillProperties.getSleepMs();
        if (sleepMs <= 0) {
            return;
        }
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("BACKFILL_SLEEP_INTERRUPTED reason={}", ex.getMessage());
        }
    }

    private void logNoProvidersOnce(String assetType) {
        if ("PRICE".equals(assetType) && !noPriceProvidersLogged) {
            noPriceProvidersLogged = true;
            log.info("No historical providers configured for assetType=PRICE. Backfill skipped.");
            return;
        }
        if ("FX".equals(assetType) && !noFxProvidersLogged) {
            noFxProvidersLogged = true;
            log.info("No historical providers configured for assetType=FX. Backfill skipped.");
            return;
        }
        if ("FUND".equals(assetType) && !noFundProvidersLogged) {
            noFundProvidersLogged = true;
            log.info("No historical providers configured for assetType=FUND. Backfill skipped.");
        }
    }

    private boolean hasProviders(String assetType) {
        return switch (assetType) {
            case "PRICE" -> !historicalPriceProviders.isEmpty();
            case "FX" -> !historicalFxProviders.isEmpty();
            case "FUND" -> !historicalFundProviders.isEmpty();
            default -> false;
        };
    }

    private void executePriceChunk(String symbol, String chunkExecutionKey, LocalDate chunkStart, LocalDate chunkEnd) {
        List<HistoricalPriceProvider> selectedProviders = selectPriceProvidersForSymbol(symbol);
        boolean anyData = false;
        for (HistoricalPriceProvider provider : selectedProviders) {
            List<HistoricalPricePoint> points = provider.fetchRange(symbol, chunkStart, chunkEnd);
            if (points == null || points.isEmpty()) {
                continue;
            }
            anyData = true;
            List<MarketPriceUpdatedEvent> events = new ArrayList<>();
            for (HistoricalPricePoint p : points) {
                events.add(new MarketPriceUpdatedEvent(
                        stableEventId(chunkExecutionKey, symbol, p.source(), p.occurredAt(), p.priceType()),
                        p.symbol(),
                        p.price(),
                        p.priceType() == null ? "MARKET" : p.priceType(),
                        p.source() == null ? "HISTORICAL" : p.source(),
                        p.occurredAt() == null ? Instant.now() : p.occurredAt(),
                        p.instrumentId()
                ));
            }
            marketHistoryWriteService.saveBatch(events);
        }
        if (!anyData) {
            throw new IllegalStateException("No historical data returned for symbol=" + symbol + " window=" + chunkStart + ".." + chunkEnd);
        }
    }

    private List<HistoricalPriceProvider> selectPriceProvidersForSymbol(String symbol) {
        if (symbol != null && symbol.toUpperCase(Locale.ROOT).endsWith("USDT")) {
            List<HistoricalPriceProvider> binanceOnly = historicalPriceProviders.stream()
                    .filter(provider -> provider instanceof BinanceHistoricalPriceProvider)
                    .toList();
            if (!binanceOnly.isEmpty()) {
                return binanceOnly;
            }
        }
        List<HistoricalPriceProvider> nonFinnhub = historicalPriceProviders.stream()
                .filter(provider -> !(provider instanceof FinnhubHistoricalPriceProvider))
                .toList();
        return nonFinnhub.isEmpty() ? historicalPriceProviders : nonFinnhub;
    }

    private void executeFxChunk(String canonicalSymbol, String chunkExecutionKey, LocalDate chunkStart, LocalDate chunkEnd) {
        for (HistoricalFxProvider provider : historicalFxProviders) {
            List<HistoricalFxPoint> points = provider.fetchRange(canonicalSymbol, chunkStart, chunkEnd);
            List<FxSnapshotUpdatedEvent> events = new ArrayList<>();
            for (HistoricalFxPoint p : points) {
                events.add(new FxSnapshotUpdatedEvent(
                        stableEventId(chunkExecutionKey, canonicalSymbol, p.source(), p.occurredAt(), "FX"),
                        p.canonicalSymbol(),
                        p.instrumentId(),
                        p.baseCurrency(),
                        p.quoteCurrency(),
                        p.bid(),
                        p.ask(),
                        p.mid(),
                        p.occurredAt(),
                        p.source()
                ));
            }
            fxHistoryWriteService.saveBatch(events);
        }
    }

    private void executeFundChunk(String fundCode, String chunkExecutionKey, LocalDate chunkStart, LocalDate chunkEnd) {
        boolean anyData = false;
        for (HistoricalFundProvider provider : historicalFundProviders) {
            List<HistoricalFundPoint> points = provider.fetchRange(fundCode, chunkStart, chunkEnd);
            if (points == null || points.isEmpty()) {
                continue;
            }
            anyData = true;
            List<FundSnapshotUpdatedEvent> events = new ArrayList<>();
            for (HistoricalFundPoint p : points) {
                events.add(new FundSnapshotUpdatedEvent(
                        stableEventId(chunkExecutionKey, fundCode, p.source(), p.occurredAt(), "FUND"),
                        p.fundCode(),
                        p.instrumentId(),
                        p.nav(),
                        p.occurredAt(),
                        p.source()
                ));
            }
            fundHistoryWriteService.saveBatch(events);
        }
        if (!anyData) {
            throw new IllegalStateException(
                    "No historical data returned for symbol=" + fundCode + " window=" + chunkStart + ".." + chunkEnd
            );
        }
    }

    private static String stableEventId(String chunkExecutionKey, String symbol, String source, Instant occurredAt, String type) {
        String raw = String.join("|",
                chunkExecutionKey == null ? "" : chunkExecutionKey,
                symbol == null ? "" : symbol,
                source == null ? "" : source,
                occurredAt == null ? "" : occurredAt.toString(),
                type == null ? "" : type);
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }

    @Transactional
    protected BackfillStateEntry getOrCreate(String assetType, String symbol) {
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
    protected void markChunkDone(BackfillStateEntry state, LocalDate chunkEnd) {
        state.setLastFetchedAt(chunkEnd.atStartOfDay().toInstant(ZoneOffset.UTC));
        state.setUpdatedAt(Instant.now());
        backfillStateRepository.save(state);
    }

    @FunctionalInterface
    private interface ChunkProcessor {
        void process(LocalDate chunkStart, LocalDate chunkEnd);
    }

    public record ChunkWindow(LocalDate start, LocalDate end) {
    }
}
