package com.company.marketdataservice.spot.infrastructure.scheduler;

import com.company.marketdataservice.bootstrap.config.FinnhubProperties;
import com.company.marketdataservice.catalog.application.InstrumentIngestScopeService;
import com.company.marketdataservice.catalog.application.InstrumentMappingService;
import com.company.marketdataservice.catalog.registry.IngestInstrumentDef;
import com.company.marketdataservice.catalog.registry.IngestProvider;
import com.company.marketdataservice.shared.observation.MarketPriceObservation;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import com.company.marketdataservice.spot.infrastructure.kafka.MarketEventPublisher;
import com.company.marketdataservice.history.infrastructure.orchestration.StockHistoryBootstrapGuard;
import com.company.marketdataservice.spot.infrastructure.provider.finnhub.FinnhubClient;
import com.company.marketdataservice.spot.infrastructure.provider.yahoo.YahooFinanceProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Polls BIST and US equity definitions from the platform registry.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "market.stock.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class StockPriceScheduler {

    private final InstrumentIngestScopeService ingestScope;
    private final StockHistoryBootstrapGuard stockHistoryBootstrapGuard;
    private final FinnhubProperties finnhubProperties;
    private final FinnhubClient finnhubClient;
    private final YahooFinanceProvider yahooFinanceProvider;
    private final MarketEventPublisher publisher;
    private final InstrumentMappingService instrumentMappingService;
    private final MeterRegistry meterRegistry;

    private final Set<String> mappingMissWarnFirstSeen = ConcurrentHashMap.newKeySet();
    private final Set<String> mappingHitCanonicalFirstSeen = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelayString = "${scheduler.stock.delay-ms:30000}")
    public void pullStockPrices() {
        log.info("STOCK SCHEDULER RUNNING");
        List<IngestInstrumentDef> equities = ingestScope.resolvePolledEquityDefinitions();
        if (equities.isEmpty()) {
            return;
        }
        for (IngestInstrumentDef def : equities) {
            try {
                String symbol = def.symbol();
                if (!stockHistoryBootstrapGuard.isLiveAllowed(symbol)) {
                    log.info("STOCK_DATA_WAITING_FOR_HISTORY symbol={}", symbol);
                    continue;
                }
                String source;
                BigDecimal price;
                if (def.provider() == IngestProvider.FINNHUB && finnhubProperties.isEnabled()) {
                    source = IngestProvider.FINNHUB.name();
                    price = finnhubClient.fetchLiveQuotePrice(symbol);
                } else if (def.provider() == IngestProvider.YAHOO) {
                    source = yahooFinanceProvider.source();
                    price = yahooFinanceProvider.fetchPrice(symbol);
                } else {
                    log.debug("STOCK_SCHEDULER_SKIP symbol={} provider={}", symbol, def.provider());
                    continue;
                }

                var observation = new MarketPriceObservation(source, symbol, price, Instant.now());
                Long instrumentId = instrumentMappingService.resolveInstrument(source, symbol).orElse(null);
                recordMappingMetrics(source, symbol, instrumentId);
                publisher.publishMarketPriceUpdated(
                        MarketPriceUpdatedEvent.of(symbol, price, "MARKET", source, instrumentId));
                log.info(
                        "STOCK_DATA_PUBLISHED source={}, symbol={}, price={}, instrumentId={}, observation={}",
                        observation.provider(),
                        observation.symbol(),
                        observation.price(),
                        instrumentId,
                        observation);
            } catch (Exception e) {
                log.error("STOCK_DATA_ERROR symbol={}, error={}", def.symbol(), e.getMessage());
            }
        }
    }

    private void recordMappingMetrics(String provider, String symbol, Long instrumentId) {
        String mappingKey = provider + "|" + symbol;
        if (instrumentId == null) {
            meterRegistry.counter(
                    "market_data_mapping_miss_total",
                    Tags.of(
                            "service", "market-data-service",
                            "provider", provider.isEmpty() ? "UNKNOWN" : provider,
                            "symbol", symbol,
                            "reason", "mapping_not_found"
                    )
            ).increment();
            if (mappingMissWarnFirstSeen.add(mappingKey)) {
                log.warn(
                        "mapping_miss_first_seen service=market-data-service provider={} symbol={} reason=mapping_not_found event_contract=instrumentSymbol_only event_canonical_instrument_id=absent domain=stock",
                        provider,
                        symbol
                );
            }
        } else if (mappingHitCanonicalFirstSeen.add(mappingKey)) {
            log.info(
                    "canonical_mapping_hit_first_seen service=market-data-service provider={} symbol={} instrumentId={} contract_note=instrumentId_is_canonical_finance_identity_when_seed_aligned event_canonical_instrument_id=present domain=stock",
                    provider,
                    symbol,
                    instrumentId
            );
        }
    }
}
