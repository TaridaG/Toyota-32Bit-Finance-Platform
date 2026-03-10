package com.company.logconsumer.consumer;

import com.company.logconsumer.metrics.KafkaProcessingMetrics;
import com.company.logconsumer.model.AppLogEvent;
import com.company.logconsumer.service.OpenSearchLogIndexer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.support.Acknowledgment;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppLogsKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final OpenSearchLogIndexer indexer;
    private final KafkaProcessingMetrics metrics;

    @KafkaListener(
            topics = "${log-consumer.topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var timer = metrics.startTimer();
        try {
            // correlationId header → MDC (distributed tracing)
            Header correlationHeader = record.headers().lastHeader("correlationId");
            if (correlationHeader != null) {
                String correlationId =
                        new String(correlationHeader.value(), StandardCharsets.UTF_8);
                MDC.put("correlationId", correlationId);
            }
            AppLogEvent event = objectMapper.readValue(record.value(), AppLogEvent.class);
            indexer.index(event);
            metrics.recordSuccess(timer);
            ack.acknowledge();
        } catch (Exception ex) {
            metrics.recordFailure(timer);
            log.error("Failed to process log event. raw={}", record.value(), ex);
            throw new RuntimeException("Log event processing failed", ex);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
