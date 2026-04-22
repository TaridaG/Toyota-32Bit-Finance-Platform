package com.company.logconsumer.consumer;

import com.company.logconsumer.metrics.KafkaProcessingMetrics;
import com.company.logconsumer.model.AppLogEvent;
import com.company.logconsumer.service.OpenSearchLogIndexer;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppLogsKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final OpenSearchLogIndexer indexer;
    private final KafkaProcessingMetrics metrics;
    private final ConcurrentMap<String, ConcurrentLinkedDeque<Instant>> skipEventsByReason =
            new ConcurrentHashMap<>();

    @KafkaListener(
            topics = "${log-consumer.topic}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record, Acknowledgment ack) {
        var timer = metrics.startTimer();
        try {
            Header correlationHeader = record.headers().lastHeader("correlationId");
            if (correlationHeader != null) {
                String correlationId =
                        new String(correlationHeader.value(), StandardCharsets.UTF_8);
                MDC.put("correlationId", correlationId);
            }
            AppLogEvent event = objectMapper.readValue(record.value(), AppLogEvent.class);
            try {
                indexer.index(event);
            } catch (Exception ex) {
                metrics.recordOpenSearchFailure(timer, "unknown", "opensearch_failure");
                throw ex;
            }
            metrics.recordSuccess(timer, "unknown");
            ack.acknowledge();
        } catch (JsonProcessingException ex) {
            metrics.recordSkipped(timer, "unknown", "malformed_json");
            log.warn("Skipping malformed log event. raw={}", record.value());
            emitSkipThresholdWarning("malformed_json");
            ack.acknowledge();
        } catch (Exception ex) {
            metrics.recordFailure(timer, "unknown", "processing_failure");
            log.error("Failed to process log event. raw={}", record.value(), ex);
            throw new RuntimeException("Log event processing failed", ex);
        } finally {
            MDC.remove("correlationId");
        }
    }

    private void emitSkipThresholdWarning(String reason) {
        int windowSec = 60;
        Instant now = Instant.now();
        Instant threshold = now.minusSeconds(windowSec);
        ConcurrentLinkedDeque<Instant> events =
                skipEventsByReason.computeIfAbsent(reason, key -> new ConcurrentLinkedDeque<>());
        events.addLast(now);
        while (!events.isEmpty() && events.peekFirst().isBefore(threshold)) {
            events.pollFirst();
        }
        if (events.size() > 10) {
            double rate = (double) events.size() / (double) windowSec;
            log.warn("ALERT_SIGNAL service=log-consumer-service metric=kafka_events_skipped_total symbol=unknown reason={} rate={} window={}s count={}",
                    reason, rate, windowSec, events.size());
        }
    }
}
