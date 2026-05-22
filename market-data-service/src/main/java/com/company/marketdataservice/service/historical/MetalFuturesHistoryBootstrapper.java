package com.company.marketdataservice.service.historical;

import com.company.marketdataservice.config.MetalFuturesHistoryBootstrapProperties;
import com.company.marketdataservice.event.MarketPriceUpdatedEvent;
import com.company.marketdataservice.historical.HistoricalPricePoint;
import com.company.marketdataservice.instrument.InstrumentMappingService;
import com.company.marketdataservice.provider.yahoo.YahooFinanceHistoricalPriceProvider;
import com.company.marketdataservice.service.history.MarketHistoryWriteService;
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
import java.util.Locale;
import java.util.UUID;

/**
 * Yahoo international metal futures ({@code GC=F}, …) need daily closes in
 * {@code mds_market_price_history} for 1M–1Y columns. Spot TRY metals use
 * {@link MetalSpotHistoryBootstrapper} + FX history; futures are priced live but
 * were not backfilled before this component.
 */
@Component
public class MetalFuturesHistoryBootstrapper {

    private static final Logger log = LoggerFactory.getLogger(MetalFuturesHistoryBootstrapper.class);
    private static final List<String> YAHOO_METAL_FUTURES = List.of("GC=F", "SI=F", "HG=F", "PA=F", "PL=F");
    private static final String PRICE_TYPE = "MARKET";

    private final YahooFinanceHistoricalPriceProvider historicalPriceProvider;
    private final MarketHistoryWriteService marketHistoryWriteService;
    private final InstrumentMappingService instrumentMappingService;
    private final MetalFuturesHistoryBootstrapProperties properties;

    public MetalFuturesHistoryBootstrapper(
            YahooFinanceHistoricalPriceProvider historicalPriceProvider,
            MarketHistoryWriteService marketHistoryWriteService,
            InstrumentMappingService instrumentMappingService,
            MetalFuturesHistoryBootstrapProperties properties
    ) {
        this.historicalPriceProvider = historicalPriceProvider;
        this.marketHistoryWriteService = marketHistoryWriteService;
        this.instrumentMappingService = instrumentMappingService;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info(
                "METAL_FUTURES_HISTORY_BOOTSTRAP_INIT enabled={} delayMs={} years={}",
                properties.isEnabled(),
                properties.getStartupDelayMs(),
                properties.getYears()
        );
        if (!properties.isEnabled()) {
            return;
        }
        Thread.ofVirtual().name("metal-futures-history-bootstrap-startup").start(() -> {
            try {
                Thread.sleep(Math.max(properties.getStartupDelayMs(), 0L));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
            backfillAll("startup");
        });
    }

    @Scheduled(
            initialDelayString = "${market.metal-futures.history-bootstrap.startup-delay-ms:90000}",
            fixedDelayString = "${market.metal-futures.history-bootstrap.periodic-delay-ms:3600000}"
    )
    public void periodic() {
        if (!properties.isEnabled()) {
            return;
        }
        backfillAll("periodic");
    }

    private void backfillAll(String trigger) {
        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        int years = Math.max(1, Math.min(10, properties.getYears()));
        LocalDate start = end.minusYears(years);
        for (String symbol : YAHOO_METAL_FUTURES) {
            try {
                backfillSymbol(symbol, start, end, trigger);
            } catch (Exception ex) {
                log.warn("METAL_FUTURES_HISTORY_BACKFILL_FAILED trigger={} symbol={} reason={}", trigger, symbol, ex.getMessage());
            }
        }
    }

    private void backfillSymbol(String symbol, LocalDate start, LocalDate end, String trigger) {
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        List<HistoricalPricePoint> raw = historicalPriceProvider.fetchRange(normalized, start, end);
        if (raw.isEmpty()) {
            log.info("METAL_FUTURES_HISTORY_EMPTY trigger={} symbol={} from={} to={}", trigger, normalized, start, end);
            return;
        }
        Long instrumentId = instrumentMappingService.resolveInstrument("YAHOO", normalized).orElse(null);
        List<MarketPriceUpdatedEvent> events = new ArrayList<>();
        for (HistoricalPricePoint point : raw) {
            if (point.price() == null) {
                continue;
            }
            String priceType = point.priceType() == null || point.priceType().isBlank()
                    ? PRICE_TYPE
                    : point.priceType().trim().toUpperCase(Locale.ROOT);
            Instant at = point.occurredAt() == null ? Instant.now() : point.occurredAt();
            events.add(new MarketPriceUpdatedEvent(
                    UUID.randomUUID().toString(),
                    normalized,
                    point.price(),
                    priceType,
                    point.source() == null ? "YAHOO" : point.source(),
                    at,
                    instrumentId
            ));
        }
        if (events.isEmpty()) {
            log.info("METAL_FUTURES_HISTORY_NO_CLOSES trigger={} symbol={}", trigger, normalized);
            return;
        }
        marketHistoryWriteService.saveBatch(events);
        log.info("METAL_FUTURES_HISTORY_BACKFILLED trigger={} symbol={} points={}", trigger, normalized, events.size());
    }
}
