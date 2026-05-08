package com.company.marketdataservice.service.historical;

import com.company.marketdataservice.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.event.FxSnapshotUpdatedEvent;
import com.company.marketdataservice.fx.history.YahooDerivedMetalHistoricalFxProvider;
import com.company.marketdataservice.historical.HistoricalFxPoint;
import com.company.marketdataservice.instrument.InstrumentMappingService;
import com.company.marketdataservice.service.history.FxHistoryWriteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MetalSpotHistoryBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(MetalSpotHistoryBootstrapper.class);
    private static final List<String> METAL_SYMBOLS = List.of("XAUTRY", "XAGTRY", "XPTTRY", "XPDTRY", "XCUTRY");

    private final YahooDerivedMetalHistoricalFxProvider metalHistoricalProvider;
    private final FxHistoryWriteService fxHistoryWriteService;
    private final InstrumentMappingService instrumentMappingService;
    private final MarketHistoryBackfillProperties backfillProperties;

    public MetalSpotHistoryBootstrapper(
            YahooDerivedMetalHistoricalFxProvider metalHistoricalProvider,
            FxHistoryWriteService fxHistoryWriteService,
            InstrumentMappingService instrumentMappingService,
            MarketHistoryBackfillProperties backfillProperties
    ) {
        this.metalHistoricalProvider = metalHistoricalProvider;
        this.fxHistoryWriteService = fxHistoryWriteService;
        this.instrumentMappingService = instrumentMappingService;
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
                List<HistoricalFxPoint> points = metalHistoricalProvider.fetchRange(symbol, start, end);
                if (points.isEmpty()) {
                    log.info("METAL_SPOT_HISTORY_EMPTY trigger={} symbol={} from={} to={}", trigger, symbol, start, end);
                    continue;
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
            } catch (Exception ex) {
                log.warn("METAL_SPOT_HISTORY_BACKFILL_FAILED trigger={} symbol={} reason={}", trigger, symbol, ex.getMessage());
            }
        }
    }
}
