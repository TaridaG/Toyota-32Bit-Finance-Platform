package com.company.analytics.infrastructure.kafka;

import com.company.analytics.application.AnalyticsAggregationService;
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

    private final AnalyticsAggregationService aggregationService;
    private final EventIdempotencyService idempotencyService;

    @KafkaListener(
            topics = "transaction-executed",
            groupId = "analytics-service"
    )
    public void consume(
            ConsumerRecord<String, TransactionExecutedEvent> record
    ) {

        String eventKey =
                record.topic() + "-" +
                        record.partition() + "-" +
                        record.offset();

        if (idempotencyService.isProcessed(eventKey)) {
            log.info("duplicate event skipped {}", eventKey);
            return;
        }

        aggregationService.process(record.value());

        idempotencyService.markProcessed(eventKey);
    }
}