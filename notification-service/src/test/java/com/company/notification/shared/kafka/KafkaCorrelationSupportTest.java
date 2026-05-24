package com.company.notification.shared.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KafkaCorrelationSupportTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void runWithCorrelation_sets_and_clears_mdc() {
        RecordHeaders headers = new RecordHeaders();
        headers.add(new RecordHeader("correlationId", "corr-123".getBytes(StandardCharsets.UTF_8)));
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "topic", 0, 0L, 0L, null, 0, 0, "k", "v", headers, null
        );

        AtomicReference<String> during = new AtomicReference<>();
        KafkaCorrelationSupport.runWithCorrelation(record, () -> during.set(MDC.get("correlationId")));

        assertEquals("corr-123", during.get());
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void runWithCorrelation_without_header() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic", 0, 0L, "k", "v");

        AtomicReference<String> during = new AtomicReference<>();
        KafkaCorrelationSupport.runWithCorrelation(record, () -> during.set(MDC.get("correlationId")));

        assertNull(during.get());
        assertNull(MDC.get("correlationId"));
    }
}
