package com.company.marketdataservice.scheduler;

import com.company.marketdataservice.config.FundMarketProperties;
import com.company.marketdataservice.event.FundSnapshotUpdatedEvent;
import com.company.marketdataservice.fund.FundBatchTelemetry;
import com.company.marketdataservice.fund.FundProvider;
import com.company.marketdataservice.fund.FundSnapshot;
import com.company.marketdataservice.instrument.InstrumentMappingService;
import com.company.marketdataservice.kafka.FundSnapshotEventPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "market.fund.scheduler-enabled", havingValue = "true")
public class FundScheduler {

    private static final String PROVIDER = "TEFAS";

    private final FundProvider fundProvider;
    private final FundMarketProperties fundMarketProperties;
    private final InstrumentMappingService instrumentMappingService;
    private final FundSnapshotEventPublisher fundSnapshotEventPublisher;
    private final MeterRegistry meterRegistry;
    private final FundBatchTelemetry fundBatchTelemetry;

    @PostConstruct
    void logSchedulerBeanActive() {
        log.info(
                "FUND_SCHEDULER_BEAN_ACTIVE trackedFundCodes={} delayMs={}",
                fundMarketProperties.getTrackedFundCodes(),
                fundMarketProperties.getDelayMs()
        );
    }

    @Scheduled(
            initialDelayString = "${market.fund.scheduler-initial-delay-ms:120000}",
            fixedDelayString = "${market.fund.delay-ms:3600000}"
    )
    public void pullFundNavs() {
        List<String> codes = fundMarketProperties.getTrackedFundCodes();
        if (codes == null || codes.isEmpty()) {
            return;
        }
        long startNanos = System.nanoTime();
        try {
            List<FundSnapshot> snapshots = fundProvider.fetchLatestNavs(codes);
            boolean usedHttpFallback = fundBatchTelemetry.consumeHttpFallback();
            if (snapshots.isEmpty()) {
                meterRegistry.counter(
                        "fund_fetch_failure_total",
                        Tags.of(
                                "service", "market-data-service",
                                "source", fundProvider.source(),
                                "reason", "no_navs"
                        )
                ).increment();
                log.warn("FUND_FETCH_EMPTY source={} reason=no_navs", fundProvider.source());
                log.info(
                        "FUND_BATCH_SUMMARY source={} requestedCount={} rawSnapshotCount=0 publishedCount=0 skippedInvalidNav=0 usedHttpFallback={} durationMs={} outcome=empty",
                        fundProvider.source(),
                        codes.size(),
                        usedHttpFallback,
                        durationMs(startNanos)
                );
                return;
            }

            int published = 0;
            int skippedInvalid = 0;
            for (FundSnapshot s : snapshots) {
                if (isInvalidNav(s.nav())) {
                    meterRegistry.counter(
                            "fund_invalid_nav_total",
                            Tags.of("service", "market-data-service", "reason", "non_positive_or_null")
                    ).increment();
                    skippedInvalid++;
                    continue;
                }
                Long instrumentId = instrumentMappingService
                        .resolveInstrument(PROVIDER, s.fundCode())
                        .orElse(null);
                FundSnapshotUpdatedEvent event = FundSnapshotUpdatedEvent.of(
                        s.fundCode(),
                        instrumentId,
                        s.nav(),
                        s.source()
                );
                fundSnapshotEventPublisher.publishFundSnapshotUpdated(event);
                published++;
                log.info(
                        "FUND_SNAPSHOT_PUBLISHED fundCode={} instrumentId={} nav={} source={}",
                        s.fundCode(),
                        instrumentId,
                        s.nav(),
                        s.source()
                );
            }

            if (published == 0) {
                meterRegistry.counter(
                        "fund_fetch_failure_total",
                        Tags.of(
                                "service", "market-data-service",
                                "source", fundProvider.source(),
                                "reason", "all_invalid_nav"
                        )
                ).increment();
                log.info(
                        "FUND_BATCH_SUMMARY source={} requestedCount={} rawSnapshotCount={} publishedCount=0 skippedInvalidNav={} usedHttpFallback={} durationMs={} outcome=all_invalid_nav",
                        fundProvider.source(),
                        codes.size(),
                        snapshots.size(),
                        skippedInvalid,
                        usedHttpFallback,
                        durationMs(startNanos)
                );
                return;
            }

            meterRegistry.counter(
                    "fund_fetch_success_total",
                    Tags.of("service", "market-data-service", "source", fundProvider.source())
            ).increment();
            log.info(
                    "FUND_BATCH_SUMMARY source={} requestedCount={} rawSnapshotCount={} publishedCount={} skippedInvalidNav={} usedHttpFallback={} durationMs={} outcome=ok",
                    fundProvider.source(),
                    codes.size(),
                    snapshots.size(),
                    published,
                    skippedInvalid,
                    usedHttpFallback,
                    durationMs(startNanos)
            );
        } catch (Exception ex) {
            meterRegistry.counter(
                    "fund_fetch_failure_total",
                    Tags.of(
                            "service", "market-data-service",
                            "source", fundProvider.source(),
                            "reason", ex.getClass().getSimpleName()
                    )
            ).increment();
            log.error("FUND_FETCH_FAILED source={} reason={}", fundProvider.source(), ex.getMessage());
            log.info(
                    "FUND_BATCH_SUMMARY source={} requestedCount={} rawSnapshotCount=0 publishedCount=0 skippedInvalidNav=0 usedHttpFallback={} durationMs={} outcome=error",
                    fundProvider.source(),
                    codes.size(),
                    fundBatchTelemetry.consumeHttpFallback(),
                    durationMs(startNanos)
            );
        }
    }

    private static boolean isInvalidNav(BigDecimal nav) {
        return nav == null || nav.compareTo(BigDecimal.ZERO) <= 0;
    }

    private static long durationMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
