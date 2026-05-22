package com.company.marketdataservice.scheduler;

import com.company.marketdataservice.config.MarketDataProperties;
import com.company.marketdataservice.config.FinnhubProperties;
import com.company.marketdataservice.config.MetalFuturesSymbols;
import com.company.marketdataservice.dto.MarketPriceDto;
import com.company.marketdataservice.provider.yahoo.YahooSpotQuote;
import com.company.marketdataservice.event.MarketPriceUpdatedEvent;
import com.company.marketdataservice.instrument.InstrumentMappingService;
import com.company.marketdataservice.kafka.MarketEventPublisher;
import com.company.marketdataservice.observation.MarketPriceObservation;
import com.company.marketdataservice.snapshot.MarketSnapshotStore;
import com.company.marketdataservice.provider.finnhub.FinnhubClient;
import com.company.marketdataservice.provider.yahoo.YahooFinanceProvider;
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
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "market.stock.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class StockPriceScheduler {

    private final MarketDataProperties properties;
    private final FinnhubProperties finnhubProperties;
    private final FinnhubClient finnhubClient;
    private final YahooFinanceProvider yahooFinanceProvider;
    private final MarketEventPublisher publisher;
    private final InstrumentMappingService instrumentMappingService;
    private final MeterRegistry meterRegistry;
    private final MarketSnapshotStore marketSnapshotStore;

    private final Set<String> mappingMissWarnFirstSeen = ConcurrentHashMap.newKeySet();
    private final Set<String> mappingHitCanonicalFirstSeen = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelayString = "${scheduler.stock.delay-ms:30000}")
    public void pullStockPrices() {
        log.info("STOCK SCHEDULER RUNNING");
        List<String> stocks = properties.getTrackedStocks();
        if (stocks == null || stocks.isEmpty()) {
            return;
        }
        for (String symbol : stocks) {
            try {
                boolean finnhubOwned = isOwnedByFinnhub(symbol);
                boolean metalFuture = !finnhubOwned && MetalFuturesSymbols.isFutures(symbol);
                String source = finnhubOwned ? "FINNHUB" : yahooFinanceProvider.source();
                BigDecimal price;
                if (finnhubOwned) {
                    price = finnhubClient.fetchLiveQuotePrice(symbol);
                } else if (metalFuture) {
                    YahooSpotQuote quote = yahooFinanceProvider.fetchSpotQuote(symbol);
                    price = quote.price();
                    marketSnapshotStore.recordMarketPriceDto(toMarketPriceDto(quote));
                } else {
                    price = yahooFinanceProvider.fetchPrice(symbol);
                }

                var observation = new MarketPriceObservation(
                        source,
                        symbol,
                        price,
                        Instant.now()
                );

                Long instrumentId = instrumentMappingService
                        .resolveInstrument(source, symbol)
                        .orElse(null);

                String provider = observation.provider() == null ? "" : observation.provider();
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
                    } else {
                        log.debug(
                                "mapping_miss_repeat service=market-data-service provider={} symbol={} reason=mapping_not_found domain=stock",
                                provider,
                                symbol
                        );
                    }
                } else {
                    if (mappingHitCanonicalFirstSeen.add(mappingKey)) {
                        log.info(
                                "canonical_mapping_hit_first_seen service=market-data-service provider={} symbol={} instrumentId={} contract_note=instrumentId_is_canonical_finance_identity_when_seed_aligned event_canonical_instrument_id=present domain=stock",
                                provider,
                                symbol,
                                instrumentId
                        );
                    }
                }

                publisher.publishMarketPriceUpdated(
                        MarketPriceUpdatedEvent.of(
                                symbol,
                                price,
                                "MARKET",
                                source,
                                instrumentId
                        )
                );

                log.info(
                        "STOCK_DATA_PUBLISHED source={}, symbol={}, price={}, instrumentId={}, observation={}",
                        observation.provider(),
                        observation.symbol(),
                        observation.price(),
                        instrumentId,
                        observation
                );
            } catch (Exception e) {
                log.error("STOCK_DATA_ERROR symbol={}, error={}", symbol, e.getMessage());
            }
        }
    }

    private static MarketPriceDto toMarketPriceDto(YahooSpotQuote quote) {
        String linkedSpot = MetalFuturesSymbols.linkedSpotTry(quote.symbol());
        return new MarketPriceDto(
                quote.symbol(),
                quote.price(),
                quote.source(),
                quote.timestamp(),
                quote.volume24h(),
                quote.openInterest(),
                quote.dayOpen(),
                quote.dayHigh(),
                quote.dayLow(),
                quote.exchangeName(),
                quote.underlyingSymbol(),
                quote.contractExpiry(),
                linkedSpot,
                null,
                null);
    }

    private boolean isOwnedByFinnhub(String symbol) {
        if (!finnhubProperties.isEnabled() || symbol == null || symbol.isBlank()) {
            return false;
        }
        Set<String> owned = finnhubProperties.getSymbols() == null
                ? Set.of()
                : finnhubProperties.getSymbols().stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return owned.contains(symbol.trim().toUpperCase(Locale.ROOT));
    }
}
