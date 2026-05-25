package com.company.logconsumer.ingestion.infrastructure.kafka;

import com.company.logconsumer.ingestion.domain.AppLogEvent;
import com.company.logconsumer.ingestion.infrastructure.opensearch.OpenSearchLogIndexer;
import com.company.logconsumer.shared.metrics.KafkaProcessingMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.kafka.support.Acknowledgment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AppLogsKafkaConsumerTest {

    @Mock
    private OpenSearchLogIndexer indexer;
    @Mock
    private Acknowledgment ack;

    private SimpleMeterRegistry registry;
    private KafkaProcessingMetrics metrics;
    private AppLogsKafkaConsumer consumer;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new KafkaProcessingMetrics(registry);
        consumer = new AppLogsKafkaConsumer(new ObjectMapper(), indexer, metrics);
    }

    @Test
    void consume_validPayload_indexesAndAcks() throws Exception {
        String json = """
                {"timestamp":"2026-05-23T10:00:00Z","level":"INFO","serviceName":"finance-api","message":"ok"}
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("app.logs", 0, 0L, "key", json);

        consumer.consume(record, ack);

        verify(indexer).index(any(AppLogEvent.class));
        verify(ack).acknowledge();
        assertEquals(1.0, registry.get("kafka_events_processed_total")
                .tag("symbol", "finance-api")
                .counter()
                .count());
    }

    @Test
    void consume_logstashServiceField_indexesAndAcks() throws Exception {
        String json = """
                {"@timestamp":"2026-05-23T10:00:00Z","level":"INFO","service":"finance-api","message":"ok"}
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("app.logs", 0, 0L, "key", json);

        consumer.consume(record, ack);

        verify(indexer).index(any(AppLogEvent.class));
        verify(ack).acknowledge();
        assertEquals(1.0, registry.get("kafka_events_processed_total")
                .tag("symbol", "finance-api")
                .counter()
                .count());
    }

    @Test
    void resolveSymbol_blankServiceName_returnsUnknown() {
        AppLogEvent event = new AppLogEvent(
                "2026-05-23T10:00:00Z", "INFO", "  ", "ok",
                null, null, null, null, null, null
        );

        assertEquals("unknown", AppLogsKafkaConsumer.resolveSymbol(event));
    }

    @Test
    void consume_malformedJson_skipsAndAcks() throws Exception {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("app.logs", 0, 0L, "key", "{bad");

        consumer.consume(record, ack);

        verify(indexer, never()).index(any());
        verify(ack).acknowledge();
    }

    @Test
    void consume_openSearchFailure_recordsMetricsAndDoesNotAck() throws Exception {
        String json = """
                {"timestamp":"2026-05-23T10:00:00Z","level":"ERROR","serviceName":"finance-api","message":"fail"}
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("app.logs", 0, 0L, "key", json);
        doThrow(new RuntimeException("opensearch down")).when(indexer).index(any());

        assertThrows(RuntimeException.class, () -> consumer.consume(record, ack));
        verify(ack, never()).acknowledge();
        assertEquals(1.0, registry.get("kafka_events_opensearch_failed_total")
                .tag("symbol", "finance-api")
                .counter()
                .count());
        assertEquals(1.0, registry.get("kafka_events_failed_total")
                .tag("symbol", "finance-api")
                .counter()
                .count());
    }

    @Test
    void consume_correlationHeader_putsMdc() throws Exception {
        String json = """
                {"timestamp":"2026-05-23T10:00:00Z","level":"INFO","serviceName":"finance-api","message":"ok"}
                """;
        ConsumerRecord<String, String> record = new ConsumerRecord<>("app.logs", 0, 0L, "key", json);
        record.headers().add(new RecordHeader("correlationId", "corr-123".getBytes()));

        consumer.consume(record, ack);

        assertNull(MDC.get("correlationId"));
        verify(ack).acknowledge();
    }
}
