package com.company.analytics.infrastructure.kafka;

import com.company.analytics.application.AnalyticsAggregationService;
import com.company.analytics.application.CandleAggregationService;
import com.company.analytics.application.EventIdempotencyService;
import com.company.analytics.event.TransactionExecutedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionExecutedConsumer {

    private final AnalyticsAggregationService analyticsAggregationService;
    private final CandleAggregationService candleAggregationService;
    private final EventIdempotencyService eventIdempotencyService;

    @KafkaListener(
            topics = "transaction-executed",
            groupId = "analytics-service",
            containerFactory = "analyticsKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, TransactionExecutedEvent> record) {
        String eventKey = record.topic()
                + "-" + record.partition()
                + "-" + record.offset();

        if (eventIdempotencyService.isProcessed(eventKey)) {
            log.info("Duplicate analytics event skipped: {}", eventKey);
            return;
        }

        TransactionExecutedEvent event = record.value();

        analyticsAggregationService.process(event);
        candleAggregationService.process(event);

        eventIdempotencyService.markProcessed(eventKey);

        log.info("Analytics processed event for symbol={} eventKey={}",
                event.getInstrumentSymbol(), eventKey);
    }
}