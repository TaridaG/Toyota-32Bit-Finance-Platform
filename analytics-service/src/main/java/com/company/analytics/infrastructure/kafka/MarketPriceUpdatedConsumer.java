package com.company.analytics.infrastructure.kafka;

import com.company.analytics.application.CandleAggregationService;
import com.company.analytics.application.EventIdempotencyService;
import com.company.analytics.application.MovingAverageService;
import com.company.analytics.application.RSIService;
import com.company.analytics.application.TrendMetricService;
import com.company.analytics.client.FinanceInstrumentClient;
import com.company.analytics.event.AnalyticsMarketPriceEvent;
import com.company.analytics.event.MarketPriceUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceUpdatedConsumer {

    private final CandleAggregationService candleAggregationService;
    private final EventIdempotencyService eventIdempotencyService;
    private final MovingAverageService movingAverageService;
    private final RSIService rsiService;
    private final TrendMetricService trendMetricService;
    private final FinanceInstrumentClient financeInstrumentClient;

    @Transactional
    @KafkaListener(
            topics = "market.price.updated",
            groupId = "analytics-service",
            containerFactory = "analyticsKafkaListenerContainerFactory"
    )
    public void consume(MarketPriceUpdatedEvent record) {
        if (eventIdempotencyService.isProcessed(record.eventId())) {
            log.info("Duplicate market event skipped: {}", record.eventId());
            return;
        }

        Long instrumentId = financeInstrumentClient.resolveInstrumentId(record.instrumentSymbol())
                .orElseThrow(() -> new IllegalStateException(
                        "Instrument not found for symbol: " + record.instrumentSymbol()
                ));

        AnalyticsMarketPriceEvent event = new AnalyticsMarketPriceEvent(
                record.eventId(),
                instrumentId,
                record.instrumentSymbol(),
                record.price(),
                record.occurredAt()
        );

        candleAggregationService.process(event);
        movingAverageService.process(event);
        rsiService.process(event);
        trendMetricService.process(event);
        eventIdempotencyService.markProcessed(event.eventId());

        log.info("Analytics processed market event for symbol={} eventId={}",
                event.instrumentSymbol(), event.eventId());
    }
}
