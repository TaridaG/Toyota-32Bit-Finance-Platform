package com.company.logconsumer.ingestion.infrastructure.kafka;

import com.company.logconsumer.ingestion.domain.AppLogEvent;
import com.company.logconsumer.ingestion.infrastructure.opensearch.OpenSearchLogIndexer;
import com.company.logconsumer.shared.metrics.KafkaProcessingMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * {@code app.logs} Kafka topic'inden gelen JSON log olaylarını parse eder ve OpenSearch'e yazar.
 * Bozuk JSON atlanır (ack); index hataları DLQ akışına bırakılır (retry + dead-letter).
 */
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
        String symbol = "unknown";
        try {
            Header correlationHeader = record.headers().lastHeader("correlationId");
            if (correlationHeader != null) {
                String correlationId =
                        new String(correlationHeader.value(), StandardCharsets.UTF_8);
                MDC.put("correlationId", correlationId);
            }
            AppLogEvent event = objectMapper.readValue(record.value(), AppLogEvent.class);
            symbol = resolveSymbol(event);
            try {
                indexer.index(event);
            } catch (Exception ex) {
                metrics.recordOpenSearchFailure(timer, symbol, "opensearch_failure");
                throw ex;
            }
            metrics.recordSuccess(timer, symbol);
            ack.acknowledge();
        } catch (JsonProcessingException ex) {
            metrics.recordSkipped(timer, symbol, "malformed_json");
            log.warn("Skipping malformed log event. raw={}", record.value());
            emitSkipThresholdWarning(symbol, "malformed_json");
            ack.acknowledge();
        } catch (Exception ex) {
            metrics.recordFailure(timer, symbol, "processing_failure");
            log.error("Failed to process log event. raw={}", record.value(), ex);
            throw new RuntimeException("Log event processing failed", ex);
        } finally {
            MDC.remove("correlationId");
        }
    }

    static String resolveSymbol(AppLogEvent event) {
        if (event == null || event.serviceName() == null || event.serviceName().isBlank()) {
            return "unknown";
        }
        return event.serviceName();
    }

    private void emitSkipThresholdWarning(String symbol, String reason) {
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
            log.warn("ALERT_SIGNAL service=log-consumer-service metric=kafka_events_skipped_total symbol={} reason={} rate={} window={}s count={}",
                    symbol, reason, rate, windowSec, events.size());
        }
    }
}
