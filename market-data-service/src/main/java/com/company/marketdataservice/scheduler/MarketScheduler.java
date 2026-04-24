package com.company.marketdataservice.scheduler;

import com.company.marketdataservice.config.MarketDataProperties;
import com.company.marketdataservice.event.MarketPriceUpdatedEvent;
import com.company.marketdataservice.instrument.InstrumentMappingService;
import com.company.marketdataservice.kafka.MarketEventPublisher;
import com.company.marketdataservice.observation.MarketPriceObservation;
import com.company.marketdataservice.provider.PriceProvider;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "market.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class MarketScheduler {

    private final MarketDataProperties properties;
    private final PriceProvider priceProvider;
    private final MarketEventPublisher publisher;
    private final InstrumentMappingService instrumentMappingService;
    private final MeterRegistry meterRegistry;

    private final Set<String> mappingMissWarnFirstSeen = ConcurrentHashMap.newKeySet();
    private final Set<String> mappingHitCanonicalFirstSeen = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelayString = "${scheduler.market.delay-ms:5000}")
    public void pullMarketData() {


        for (String symbol : properties.getTrackedSymbols()) {

            try {

                BigDecimal price = priceProvider.fetchPrice(symbol);

                var observation = new MarketPriceObservation(
                        priceProvider.source(),
                        symbol,
                        price,
                        Instant.now()
                );

                Long instrumentId = instrumentMappingService
                        .resolveInstrument(priceProvider.source(), symbol)
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
                                "mapping_miss_first_seen service=market-data-service provider={} symbol={} reason=mapping_not_found event_contract=instrumentSymbol_only event_canonical_instrument_id=absent",
                                provider,
                                symbol
                        );
                    } else {
                        log.debug(
                                "mapping_miss_repeat service=market-data-service provider={} symbol={} reason=mapping_not_found",
                                provider,
                                symbol
                        );
                    }
                } else {
                    if (mappingHitCanonicalFirstSeen.add(mappingKey)) {
                        log.info(
                                "canonical_mapping_hit_first_seen service=market-data-service provider={} symbol={} instrumentId={} contract_note=instrumentId_is_canonical_finance_identity_when_seed_aligned event_canonical_instrument_id=present",
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
                                priceProvider.source(),
                                instrumentId
                        )
                );

                log.info(
                        "MARKET_DATA_PUBLISHED source={}, symbol={}, price={}, instrumentId={}, mapping_contract={}, observation={}",
                        observation.provider(),
                        observation.symbol(),
                        observation.price(),
                        instrumentId,
                        instrumentId == null ? "instrumentSymbol_only" : "canonical_instrument_id_optional",
                        observation
                );

            } catch (Exception e) {

                log.error("MARKET_DATA_ERROR symbol={}, error={}",
                        symbol, e.getMessage());

            }
        }
    }
}
