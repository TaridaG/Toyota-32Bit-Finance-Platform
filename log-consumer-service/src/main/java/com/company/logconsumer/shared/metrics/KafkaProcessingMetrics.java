package com.company.logconsumer.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

/**
 * Log ingestion pipeline metrikleri: işlenen, atlanan, başarısız ve DLQ sayaçları.
 */
@Component
public class KafkaProcessingMetrics {

    private static final Logger log = LoggerFactory.getLogger(KafkaProcessingMetrics.class);
    private static final int ALERT_WINDOW_SEC = 60;

    private final MeterRegistry meterRegistry;
    private final Timer processingTimer;
    private final ConcurrentMap<String, ConcurrentLinkedDeque<Instant>> dlqByReason = new ConcurrentHashMap<>();

    public KafkaProcessingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.processingTimer =
                Timer.builder("kafka_event_processing_latency")
                        .description("Kafka event processing latency")
                        .register(meterRegistry);
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordSuccess(Timer.Sample sample, String symbol) {
        incrementTaggedCounter("kafka_events_processed_total", symbol, "processed");
        sample.stop(processingTimer);
    }

    public void recordFailure(Timer.Sample sample, String symbol, String reason) {
        incrementTaggedCounter("kafka_events_failed_total", symbol, reason);
        sample.stop(processingTimer);
    }

    public void recordSkipped(Timer.Sample sample, String symbol, String reason) {
        incrementTaggedCounter("kafka_events_skipped_total", symbol, reason);
        sample.stop(processingTimer);
    }

    public void recordOpenSearchFailure(Timer.Sample sample, String symbol, String reason) {
        incrementTaggedCounter("kafka_events_opensearch_failed_total", symbol, reason);
        sample.stop(processingTimer);
    }

    public void recordDlqPublished(String symbol, String reason) {
        incrementTaggedCounter("kafka_events_dlq_published_total", symbol, reason);
        emitDlqAlertIfNeeded(symbol, reason);
    }

    private void emitDlqAlertIfNeeded(String symbol, String reason) {
        String key = reason + ":" + symbol;
        Instant now = Instant.now();
        Instant threshold = now.minusSeconds(ALERT_WINDOW_SEC);
        ConcurrentLinkedDeque<Instant> events = dlqByReason.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        events.addLast(now);
        while (!events.isEmpty() && events.peekFirst().isBefore(threshold)) {
            events.pollFirst();
        }
        if (events.size() > 5) {
            double rate = (double) events.size() / (double) ALERT_WINDOW_SEC;
            String safe = (symbol == null || symbol.isBlank()) ? "unknown" : symbol;
            log.warn("ALERT_SIGNAL service=log-consumer-service metric=kafka_events_dlq_published_total symbol={} reason={} rate={} window={}s count={}",
                    safe, reason, rate, ALERT_WINDOW_SEC, events.size());
        }
    }

    private void incrementTaggedCounter(String metricName, String symbol, String reason) {
        String safeSymbol = (symbol == null || symbol.isBlank()) ? "unknown" : symbol;
        String safeReason = (reason == null || reason.isBlank()) ? "unknown" : reason;
        Counter.builder(metricName)
                .tag("service", "log-consumer-service")
                .tag("symbol", safeSymbol)
                .tag("reason", safeReason)
                .register(meterRegistry)
                .increment();
    }
}
