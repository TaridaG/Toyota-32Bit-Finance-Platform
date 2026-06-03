package com.company.marketdataservice.history.infrastructure.orchestration;
import com.company.marketdataservice.bootstrap.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.fx.domain.FxSnapshotUpdatedEvent;
import com.company.marketdataservice.fx.infrastructure.provider.history.YahooDerivedMetalHistoricalFxProvider;
import com.company.marketdataservice.history.domain.HistoricalFxPoint;
import com.company.marketdataservice.catalog.application.InstrumentMappingService;
import com.company.marketdataservice.history.infrastructure.persistence.FxRateHistoryRepository;
import com.company.marketdataservice.history.infrastructure.write.FxHistoryWriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Backfills TRY spot metal FX history from Yahoo futures (GC=F, …) × USDTRY.
 */
@Component
public class MetalSpotHistoryBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(MetalSpotHistoryBootstrapper.class);
    private static final List<String> METAL_SYMBOLS = List.of("XAUTRY", "XAGTRY", "XPTTRY", "XPDTRY", "XCUTRY");
    private static final String YAHOO_DERIVED_PROVIDER = "YAHOO_DERIVED_SPOT";
    /** Rolling refresh window so recent days stay aligned with live Minted/Stooq scale. */
    private static final int RECENT_REFRESH_DAYS = 30;

    private final YahooDerivedMetalHistoricalFxProvider metalHistoricalProvider;
    private final FxHistoryWriteService fxHistoryWriteService;
    private final InstrumentMappingService instrumentMappingService;
    private final FxRateHistoryRepository fxRateHistoryRepository;
    private final MarketHistoryBackfillProperties backfillProperties;
    private final Set<String> readySymbols = ConcurrentHashMap.newKeySet();

    public MetalSpotHistoryBootstrapper(
            YahooDerivedMetalHistoricalFxProvider metalHistoricalProvider,
            FxHistoryWriteService fxHistoryWriteService,
            InstrumentMappingService instrumentMappingService,
            FxRateHistoryRepository fxRateHistoryRepository,
            MarketHistoryBackfillProperties backfillProperties
    ) {
        this.metalHistoricalProvider = metalHistoricalProvider;
        this.fxHistoryWriteService = fxHistoryWriteService;
        this.instrumentMappingService = instrumentMappingService;
        this.fxRateHistoryRepository = fxRateHistoryRepository;
        this.backfillProperties = backfillProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("METAL_SPOT_HISTORY_BOOTSTRAP_INIT enabled={} runOnStartup={}",
                backfillProperties.isEnabled(),
                backfillProperties.isRunOnStartup());
        if (!backfillProperties.isEnabled()) {
            return;
        }
        backfillMetals("startup");
    }

    @Scheduled(initialDelayString = "${market.history.backfill.schedule-initial-delay-ms:30000}",
            fixedDelayString = "${scheduler.fx.delay-ms:300000}")
    public void periodic() {
        if (!backfillProperties.isEnabled()) {
            return;
        }
        backfillMetals("periodic");
    }

    private void backfillMetals(String trigger) {
        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        int years = Math.max(1, Math.min(10, backfillProperties.getYears()));
        LocalDate start = end.minusYears(years);
        for (String symbol : METAL_SYMBOLS) {
            try {
                backfillSymbol(symbol, start, end, trigger);
            } catch (Exception ex) {
                log.warn("METAL_SPOT_HISTORY_BACKFILL_FAILED trigger={} symbol={} reason={}", trigger, symbol, ex.getMessage());
            }
        }
    }

    private void backfillSymbol(String symbol, LocalDate start, LocalDate end, String trigger) {
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        if ("periodic".equals(trigger) && hasSufficientHistory(normalized)) {
            LocalDate recentStart = end.minusDays(RECENT_REFRESH_DAYS);
            if (!recentStart.isBefore(start)) {
                refreshRecentRange(normalized, recentStart, end, trigger);
            }
            return;
        }
        if (readySymbols.contains(normalized) || hasSufficientHistory(normalized)) {
            readySymbols.add(normalized);
            log.debug("METAL_SPOT_HISTORY_SKIP trigger={} symbol={} reason=sufficient_history", trigger, normalized);
            return;
        }
        persistRange(normalized, start, end, trigger);
        if (hasSufficientHistory(normalized)) {
            readySymbols.add(normalized);
        }
    }

    protected void refreshRecentRange(String symbol, LocalDate start, LocalDate end, String trigger) {
        Instant fromInclusive = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        int deleted = fxRateHistoryRepository.deleteBySymbolProviderAndObservedAtBetween(
                symbol,
                YAHOO_DERIVED_PROVIDER,
                fromInclusive,
                toExclusive
        );
        persistRange(symbol, start, end, trigger + "_recent");
        log.info(
                "METAL_SPOT_HISTORY_REFRESHED trigger={} symbol={} deleted={} from={} to={}",
                trigger,
                symbol,
                deleted,
                start,
                end
        );
    }

    private void persistRange(String symbol, LocalDate start, LocalDate end, String trigger) {
        List<HistoricalFxPoint> points = metalHistoricalProvider.fetchRange(symbol, start, end);
        if (points.isEmpty()) {
            log.info("METAL_SPOT_HISTORY_EMPTY trigger={} symbol={} from={} to={}", trigger, symbol, start, end);
            return;
        }
        Long instrumentId = instrumentMappingService.resolveInstrument("COMPOSITE_FX", symbol).orElse(null);
        List<FxSnapshotUpdatedEvent> events = new ArrayList<>(points.size());
        for (HistoricalFxPoint p : points) {
            events.add(new FxSnapshotUpdatedEvent(
                    UUID.randomUUID().toString(),
                    p.canonicalSymbol(),
                    instrumentId,
                    p.baseCurrency(),
                    p.quoteCurrency(),
                    p.bid(),
                    p.ask(),
                    p.mid(),
                    p.occurredAt() == null ? Instant.now() : p.occurredAt(),
                    p.source()
            ));
        }
        fxHistoryWriteService.saveBatch(events);
        log.info("METAL_SPOT_HISTORY_BACKFILLED trigger={} symbol={} points={}", trigger, symbol, events.size());
    }

    private boolean hasSufficientHistory(String symbol) {
        long totalDays = fxRateHistoryRepository.countDistinctDaysBySymbol(symbol);
        int minTotalDays = Math.max(30, backfillProperties.getMinPriceHistoryDays());
        if (totalDays < minTotalDays) {
            return false;
        }
        Instant recentFrom = Instant.now().minus(365, ChronoUnit.DAYS);
        long recentDays = fxRateHistoryRepository.countDistinctDaysBySymbolSince(symbol, recentFrom);
        int minRecentDays = Math.max(30, backfillProperties.getMinRecentPriceHistoryDays());
        return recentDays >= minRecentDays;
    }
}
