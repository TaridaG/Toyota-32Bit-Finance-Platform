package com.company.logconsumer.shared.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KafkaProcessingMetricsTest {

    private SimpleMeterRegistry registry;
    private KafkaProcessingMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new KafkaProcessingMetrics(registry);
    }

    @Test
    void recordSuccess_incrementsProcessedCounter() {
        var sample = metrics.startTimer();

        metrics.recordSuccess(sample, "finance-api");

        assertEquals(1.0, registry.get("kafka_events_processed_total")
                .tag("service", "log-consumer-service")
                .tag("symbol", "finance-api")
                .tag("reason", "processed")
                .counter()
                .count());
    }

    @Test
    void recordSkipped_incrementsSkippedCounter() {
        var sample = metrics.startTimer();

        metrics.recordSkipped(sample, null, "malformed_json");

        assertEquals(1.0, registry.get("kafka_events_skipped_total")
                .tag("symbol", "unknown")
                .tag("reason", "malformed_json")
                .counter()
                .count());
    }

    @Test
    void recordFailure_incrementsFailedCounter() {
        var sample = metrics.startTimer();

        metrics.recordFailure(sample, "finance-api", "processing_failure");

        assertEquals(1.0, registry.get("kafka_events_failed_total")
                .tag("symbol", "finance-api")
                .tag("reason", "processing_failure")
                .counter()
                .count());
    }

    @Test
    void recordOpenSearchFailure_incrementsOpenSearchCounter() {
        var sample = metrics.startTimer();

        metrics.recordOpenSearchFailure(sample, "finance-api", "opensearch_failure");

        assertEquals(1.0, registry.get("kafka_events_opensearch_failed_total")
                .tag("symbol", "finance-api")
                .tag("reason", "opensearch_failure")
                .counter()
                .count());
    }

    @Test
    void recordDlqPublished_incrementsDlqCounter() {
        metrics.recordDlqPublished("unknown", "RuntimeException");

        assertEquals(1.0, registry.get("kafka_events_dlq_published_total")
                .tag("reason", "RuntimeException")
                .counter()
                .count());
    }
}
