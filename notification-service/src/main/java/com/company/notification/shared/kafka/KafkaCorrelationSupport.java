package com.company.notification.shared.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;

/**
 * Kafka {@code correlationId} header'larını log trace için SLF4J MDC'ye taşır.
 */
public final class KafkaCorrelationSupport {

    private KafkaCorrelationSupport() {
    }

    /**
     * Record header'larından MDC doldurarak verilen action'ı çalıştırır, ardından MDC'yi temizler.
     */
    public static void runWithCorrelation(ConsumerRecord<?, ?> record, Runnable action) {
        try {
            Header correlationHeader = record.headers().lastHeader("correlationId");
            if (correlationHeader != null) {
                String correlationId = new String(correlationHeader.value(), StandardCharsets.UTF_8);
                MDC.put("correlationId", correlationId);
            }
            action.run();
        } finally {
            MDC.remove("correlationId");
        }
    }
}
