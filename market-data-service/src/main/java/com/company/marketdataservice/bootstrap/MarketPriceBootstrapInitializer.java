package com.company.marketdataservice.bootstrap;

import com.company.marketdataservice.config.MarketDataProperties;
import com.company.marketdataservice.event.MarketPriceUpdatedEvent;
import com.company.marketdataservice.history.MarketPriceHistoryRepository;
import com.company.marketdataservice.service.history.MarketHistoryWriteService;
import com.company.marketdataservice.snapshot.MarketSnapshotStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Component
public class MarketPriceBootstrapInitializer {

    private static final Logger log = LoggerFactory.getLogger(MarketPriceBootstrapInitializer.class);

    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final MarketHistoryWriteService marketHistoryWriteService;
    private final MarketSnapshotStore marketSnapshotStore;
    private final MarketDataProperties marketDataProperties;

    public MarketPriceBootstrapInitializer(
            MarketPriceHistoryRepository marketPriceHistoryRepository,
            MarketHistoryWriteService marketHistoryWriteService,
            MarketSnapshotStore marketSnapshotStore,
            MarketDataProperties marketDataProperties
    ) {
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
        this.marketHistoryWriteService = marketHistoryWriteService;
        this.marketSnapshotStore = marketSnapshotStore;
        this.marketDataProperties = marketDataProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapIfEmpty() {
        if (marketPriceHistoryRepository.count() > 0) {
            return;
        }
        Set<String> symbols = new LinkedHashSet<>();
        if (marketDataProperties.getTrackedSymbols() != null) {
            symbols.addAll(marketDataProperties.getTrackedSymbols());
        }
        if (marketDataProperties.getTrackedStocks() != null) {
            symbols.addAll(marketDataProperties.getTrackedStocks());
        }
        if (symbols.isEmpty()) {
            return;
        }

        List<MarketPriceUpdatedEvent> seedEvents = new ArrayList<>();
        Instant now = Instant.now();
        int idx = 1;
        for (String symbolRaw : symbols) {
            if (symbolRaw == null || symbolRaw.isBlank()) {
                continue;
            }
            String symbol = symbolRaw.trim().toUpperCase(Locale.ROOT);
            BigDecimal dummyPrice = BigDecimal.valueOf(100L + idx);
            MarketPriceUpdatedEvent event = new MarketPriceUpdatedEvent(
                    UUID.randomUUID().toString(),
                    symbol,
                    dummyPrice,
                    "MARKET",
                    "BOOTSTRAP",
                    now,
                    null
            );
            seedEvents.add(event);
            marketSnapshotStore.recordMarketPrice(event);
            idx++;
        }
        if (seedEvents.isEmpty()) {
            return;
        }
        marketHistoryWriteService.saveBatch(seedEvents);
        log.info("BOOTSTRAP_SEED_INSERTED count={}", seedEvents.size());
    }
}
