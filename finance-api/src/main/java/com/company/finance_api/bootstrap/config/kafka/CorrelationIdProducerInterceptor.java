package com.company.finance_api.bootstrap.config.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.MDC;

/** MDC'deki correlation id'yi Kafka producer record header'ına ekleyen interceptor. */
public class CorrelationIdProducerInterceptor implements ProducerInterceptor<String, Object> {

  public static final String HEADER_CORRELATION_ID = "correlationId";

  /** Gönderim öncesi MDC correlation id'yi record header'ına yazar. */
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
