package com.company.marketdataservice.fx.infrastructure.scheduler;
import com.company.marketdataservice.fx.domain.FxSnapshotUpdatedEvent;
import com.company.marketdataservice.fx.domain.FxProvider;
import com.company.marketdataservice.fx.domain.FxSnapshot;
import com.company.marketdataservice.catalog.application.InstrumentMappingService;
import com.company.marketdataservice.fx.infrastructure.kafka.FxSnapshotEventPublisher;
import com.company.marketdataservice.history.infrastructure.orchestration.FxHistoryBootstrapGuard;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * `FX spot` verisini periyodik olarak fetch edip snapshot/Kafka'ya publish eden scheduler.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "market.fx.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class FxScheduler {

    private final FxProvider fxProvider;
    private final InstrumentMappingService instrumentMappingService;
    private final FxSnapshotEventPublisher fxSnapshotEventPublisher;
    private final MeterRegistry meterRegistry;
    private final FxHistoryBootstrapGuard fxHistoryBootstrapGuard;

    private final AtomicInteger lastPublishedRateCount = new AtomicInteger(0);

    private final Set<String> fxMappingMissWarnFirstSeen = ConcurrentHashMap.newKeySet();
    private final Set<String> fxMappingHitCanonicalFirstSeen = ConcurrentHashMap.newKeySet();

    @PostConstruct
    void registerFxRateGauge() {
        Gauge.builder("fx_rate_count", lastPublishedRateCount, AtomicInteger::get)
                .tags(Tags.of("service", "market-data-service", "source", fxProvider.source()))
                .register(meterRegistry);
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @Scheduled(fixedDelayString = "${scheduler.fx.delay-ms:300000}")
    public void pullFxSnapshots() {
        long startNanos = System.nanoTime();
        try {
            List<FxSnapshot> snapshots = fxProvider.fetchLatestRates();
            if (snapshots.isEmpty()) {
                meterRegistry.counter(
                        "fx_fetch_failure_total",
                        Tags.of(
                                "service", "market-data-service",
                                "source", fxProvider.source(),
                                "reason", "no_rates"
                        )
                ).increment();
                log.warn(
                        "FX_FETCH_EMPTY source={} reason=no_rates_or_parse",
                        fxProvider.source()
                );
                lastPublishedRateCount.set(0);
                log.info(
                        "FX_BATCH_SUMMARY source={} publishedCount={} symbols={} durationMs={} outcome=empty",
                        fxProvider.source(),
                        0,
                        "",
                        durationMs(startNanos)
                );
                return;
            }

            meterRegistry.counter(
                    "fx_fetch_success_total",
                    Tags.of("service", "market-data-service", "source", fxProvider.source())
            ).increment();
            lastPublishedRateCount.set(snapshots.size());

            for (FxSnapshot s : snapshots) {
                boolean bypassHistoryGate = "STOOQ_SPOT".equalsIgnoreCase(s.source());
                if (!bypassHistoryGate && !fxHistoryBootstrapGuard.isLiveAllowed(s.canonicalSymbol())) {
                    log.info("FX_DATA_WAITING_FOR_HISTORY symbol={}", s.canonicalSymbol());
                    continue;
                }
                Long instrumentId = instrumentMappingService
                        .resolveInstrument(fxProvider.source(), s.canonicalSymbol())
                        .orElse(null);

                String provider = fxProvider.source() == null ? "" : fxProvider.source();
                String mappingKey = provider + "|" + s.canonicalSymbol();
                if (instrumentId == null) {
                    meterRegistry.counter(
                            "market_data_mapping_miss_total",
                            Tags.of(
                                    "service", "market-data-service",
                                    "provider", provider.isEmpty() ? "UNKNOWN" : provider,
                                    "symbol", s.canonicalSymbol(),
                                    "reason", "mapping_not_found"
                            )
                    ).increment();
                    if (fxMappingMissWarnFirstSeen.add(mappingKey)) {
                        log.warn(
                                "mapping_miss_first_seen service=market-data-service provider={} symbol={} reason=mapping_not_found event_contract=instrumentSymbol_only event_canonical_instrument_id=absent domain=fx",
                                provider,
                                s.canonicalSymbol()
                        );
                    } else {
                        log.debug(
                                "mapping_miss_repeat service=market-data-service provider={} symbol={} reason=mapping_not_found domain=fx",
                                provider,
                                s.canonicalSymbol()
                        );
                    }
                } else {
                    if (fxMappingHitCanonicalFirstSeen.add(mappingKey)) {
                        log.info(
                                "canonical_mapping_hit_first_seen service=market-data-service provider={} symbol={} instrumentId={} contract_note=instrumentId_is_canonical_finance_identity_when_seed_aligned event_canonical_instrument_id=present domain=fx",
                                provider,
                                s.canonicalSymbol(),
                                instrumentId
                        );
                    }
                }

                FxSnapshotUpdatedEvent event = FxSnapshotUpdatedEvent.of(
                        s.canonicalSymbol(),
                        instrumentId,
                        s.baseCurrency(),
                        s.quoteCurrency(),
                        s.bid(),
                        s.ask(),
                        s.mid(),
                        s.source()
                );
                fxSnapshotEventPublisher.publishFxSnapshotUpdated(event);
                log.info(
                        "FX_SNAPSHOT_PUBLISHED symbol={} instrumentId={} mid={} source={}",
                        s.canonicalSymbol(),
                        instrumentId,
                        s.mid(),
                        s.source()
                );
            }

            String symbolsJoined = snapshots.stream()
                    .map(FxSnapshot::canonicalSymbol)
                    .collect(Collectors.joining(","));
            log.info(
                    "FX_BATCH_SUMMARY source={} publishedCount={} symbols={} durationMs={} outcome=ok",
                    fxProvider.source(),
                    snapshots.size(),
                    symbolsJoined,
                    durationMs(startNanos)
            );
        } catch (Exception ex) {
            meterRegistry.counter(
                    "fx_fetch_failure_total",
                    Tags.of(
                            "service", "market-data-service",
                            "source", fxProvider.source(),
                            "reason", ex.getClass().getSimpleName()
                    )
            ).increment();
            log.error("FX_FETCH_FAILED source={} reason={}", fxProvider.source(), ex.getMessage());
            lastPublishedRateCount.set(0);
            log.info(
                    "FX_BATCH_SUMMARY source={} publishedCount={} symbols={} durationMs={} outcome=error",
                    fxProvider.source(),
                    0,
                    "",
                    durationMs(startNanos)
            );
        }
    }

    private static long durationMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
