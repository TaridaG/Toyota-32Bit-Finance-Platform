package com.company.analytics.infrastructure.kafka;

import com.company.analytics.application.CandleAggregationService;
import com.company.analytics.application.AnalyticsProcessingDecision;
import com.company.analytics.application.AnalyticsProcessingRouter;
import com.company.analytics.application.EventIdempotencyService;
import com.company.analytics.application.MovingAverageService;
import com.company.analytics.application.RSIService;
import com.company.analytics.application.TrendMetricService;
import com.company.analytics.client.FinanceInstrumentClient;
import com.company.analytics.event.AnalyticsMarketPriceEvent;
import com.company.analytics.event.FxSnapshotUpdatedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
public class FxSnapshotUpdatedConsumer {

    private final CandleAggregationService candleAggregationService;
    private final EventIdempotencyService eventIdempotencyService;
    private final MovingAverageService movingAverageService;
    private final RSIService rsiService;
    private final TrendMetricService trendMetricService;
    private final AnalyticsProcessingRouter processingRouter;
    private final FinanceInstrumentClient financeInstrumentClient;
    private final MeterRegistry meterRegistry;
    private final boolean processingPolicyEnforced;

    public FxSnapshotUpdatedConsumer(
            CandleAggregationService candleAggregationService,
            EventIdempotencyService eventIdempotencyService,
            MovingAverageService movingAverageService,
            RSIService rsiService,
            TrendMetricService trendMetricService,
            AnalyticsProcessingRouter processingRouter,
            FinanceInstrumentClient financeInstrumentClient,
            MeterRegistry meterRegistry,
            @Value("${analytics.processing.policy-enforced:true}") boolean processingPolicyEnforced
    ) {
        this.candleAggregationService = candleAggregationService;
        this.eventIdempotencyService = eventIdempotencyService;
        this.movingAverageService = movingAverageService;
        this.rsiService = rsiService;
        this.trendMetricService = trendMetricService;
        this.processingRouter = processingRouter;
        this.financeInstrumentClient = financeInstrumentClient;
        this.meterRegistry = meterRegistry;
        this.processingPolicyEnforced = processingPolicyEnforced;
    }

    @Transactional
    @KafkaListener(
            topics = "market.fx.snapshot.updated",
            groupId = "analytics-service-fx-snapshot",
            containerFactory = "analyticsFxSnapshotKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, FxSnapshotUpdatedEvent> record) {
        if (record.value() == null) {
            return;
        }
        FxSnapshotUpdatedEvent e = record.value();
        if (e.eventId() == null || e.eventId().isBlank()) {
            return;
        }
        if (eventIdempotencyService.isProcessed(e.eventId())) {
            log.debug("Duplicate FX snapshot skipped eventId={}", e.eventId());
            return;
        }
        Optional<BigDecimal> priceOpt = selectFxPrice(e);
        if (priceOpt.isEmpty()) {
            eventIdempotencyService.markProcessed(e.eventId());
            return;
        }
        Long instrumentId = resolveInstrumentId(e);
        if (instrumentId == null) {
            eventIdempotencyService.markProcessed(e.eventId());
            return;
        }
        Instant occurredAt = e.occurredAt() != null ? e.occurredAt() : Instant.now();
        String symbol = e.canonicalSymbol() != null ? e.canonicalSymbol() : "";
        AnalyticsMarketPriceEvent event = new AnalyticsMarketPriceEvent(
                e.eventId(),
                instrumentId,
                symbol,
                priceOpt.get(),
                occurredAt,
                "FX_MID"
        );
        String normalizedPriceType = processingRouter.normalizePriceType(event.priceType());
        meterRegistry.counter(
                "analytics_event_received_total",
                Tags.of(
                        "service", "analytics-service",
                        "priceType", normalizedPriceType,
                        "sourceConsumer", "fx_snapshot"
                )
        ).increment();
        AnalyticsProcessingDecision decision = processingRouter.decide(event.priceType());
        processByDecision(event, normalizedPriceType, decision);
        eventIdempotencyService.markProcessed(e.eventId());
        meterRegistry.counter(
                "analytics_fx_snapshot_consumed_total",
                Tags.of("service", "analytics-service")
        ).increment();
        log.info("Analytics processed FX snapshot symbol={} instrumentId={} eventId={}",
                symbol, instrumentId, e.eventId());
    }

    private Long resolveInstrumentId(FxSnapshotUpdatedEvent e) {
        if (e.instrumentId() != null) {
            Optional<String> dbSymbol = financeInstrumentClient.getSymbolForInstrumentId(e.instrumentId());
            if (dbSymbol.isEmpty()) {
                return financeInstrumentClient.resolveInstrumentId(e.canonicalSymbol()).orElse(null);
            }
            String eventSymbol = e.canonicalSymbol();
            if (eventSymbol != null && !eventSymbol.isBlank() && !dbSymbol.get().equals(eventSymbol)) {
                meterRegistry.counter(
                        "instrument_symbol_mismatch_total",
                        Tags.of("service", "analytics-service", "reason", "symbol_mismatch")
                ).increment();
            }
            return e.instrumentId();
        }
        return financeInstrumentClient.resolveInstrumentId(e.canonicalSymbol()).orElse(null);
    }

    private static Optional<BigDecimal> selectFxPrice(FxSnapshotUpdatedEvent e) {
        if (e.mid() != null && e.mid().compareTo(BigDecimal.ZERO) > 0) {
            return Optional.of(e.mid());
        }
        if (e.bid() != null && e.bid().compareTo(BigDecimal.ZERO) > 0) {
            return Optional.of(e.bid());
        }
        if (e.ask() != null && e.ask().compareTo(BigDecimal.ZERO) > 0) {
            return Optional.of(e.ask());
        }
        return Optional.empty();
    }

    private void processByDecision(
            AnalyticsMarketPriceEvent event,
            String normalizedPriceType,
            AnalyticsProcessingDecision decision
    ) {
        invokeProcessor("candle", normalizedPriceType, decision.processCandle(), () -> candleAggregationService.process(event));
        invokeProcessor("moving_average", normalizedPriceType, decision.processMovingAverage(), () -> movingAverageService.process(event));
        invokeProcessor("rsi", normalizedPriceType, decision.processRsi(), () -> rsiService.process(event));
        invokeProcessor("trend", normalizedPriceType, decision.processTrend(), () -> trendMetricService.process(event));
    }

    private void invokeProcessor(String processor, String priceType, boolean allowed, Runnable runnable) {
        boolean shouldProcess = allowed || !processingPolicyEnforced;
        meterRegistry.counter(
                "analytics_processing_decision_total",
                Tags.of(
                        "service", "analytics-service",
                        "priceType", priceType,
                        "processor", processor,
                        "decision", shouldProcess ? "processed" : "skipped"
                )
        ).increment();
        if (shouldProcess) {
            runnable.run();
        }
    }
}
