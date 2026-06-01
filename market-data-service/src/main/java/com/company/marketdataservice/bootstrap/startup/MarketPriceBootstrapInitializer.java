package com.company.marketdataservice.bootstrap.startup;
import com.company.marketdataservice.bootstrap.config.MarketDataProperties;
import com.company.marketdataservice.catalog.application.InstrumentIngestScopeService;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.history.infrastructure.write.MarketHistoryWriteService;
import com.company.marketdataservice.spot.infrastructure.snapshot.MarketSnapshotStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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

/**
 * Geçmiş fiyat tablosu boşken takip edilen kripto/hisse sembolleri için sahte seed fiyat yazar.
 * <p>{@link ApplicationReadyEvent} sonrası çalışır; {@code market.price-bootstrap.enabled=false} ile kapatılabilir.</p>
 */
@Component
@ConditionalOnProperty(prefix = "market.price-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MarketPriceBootstrapInitializer {

    private static final Logger log = LoggerFactory.getLogger(MarketPriceBootstrapInitializer.class);

    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final MarketHistoryWriteService marketHistoryWriteService;
    private final MarketSnapshotStore marketSnapshotStore;
    private final MarketDataProperties marketDataProperties;
    private final InstrumentIngestScopeService ingestScope;

    public MarketPriceBootstrapInitializer(
            MarketPriceHistoryRepository marketPriceHistoryRepository,
            MarketHistoryWriteService marketHistoryWriteService,
            MarketSnapshotStore marketSnapshotStore,
            MarketDataProperties marketDataProperties,
            InstrumentIngestScopeService ingestScope
    ) {
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
        this.marketHistoryWriteService = marketHistoryWriteService;
        this.marketSnapshotStore = marketSnapshotStore;
        this.marketDataProperties = marketDataProperties;
        this.ingestScope = ingestScope;
    }

    /**
     * {@code mds_market_price_history} boşsa registry'deki sembollere BOOTSTRAP kaynağıyla dummy fiyat ekler.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapIfEmpty() {
        if (marketPriceHistoryRepository.count() > 0) {
            return;
        }
        Set<String> symbols = new LinkedHashSet<>();
        symbols.addAll(ingestScope.resolveTrackedCryptoSymbols());
        symbols.addAll(ingestScope.resolveTrackedStockSymbols());
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
