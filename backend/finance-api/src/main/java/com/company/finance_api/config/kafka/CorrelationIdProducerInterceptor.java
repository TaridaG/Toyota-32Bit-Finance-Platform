package com.company.finance_api.config.kafka;

import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CorrelationIdProducerInterceptor implements ProducerInterceptor<String, Object> {

    public static final String HEADER_CORRELATION_ID = "correlationId";

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        String correlationId = MDC.get(HEADER_CORRELATION_ID);
        if (correlationId != null && !correlationId.isBlank()) {
            record.headers().add(HEADER_CORRELATION_ID, correlationId.getBytes(StandardCharsets.UTF_8));
        }
        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {}

    @Override
    public void close() {}

    @Override
    public void configure(Map<String, ?> configs) {}
}