package com.company.logconsumer.ingestion.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kafka {@code app.logs} topic'inden gelen tek bir uygulama log satırının JSON modeli.
 * <p>
 * Üretici tarafında Logstash {@link net.logstash.logback.encoder.LogstashEncoder} alan adları
 * ({@code @timestamp}, {@code service}, {@code logger_name}) ile uyumludur.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppLogEvent(
        @JsonProperty("@timestamp") String timestamp,
        String level,
        @JsonProperty("service") @JsonAlias("serviceName") String serviceName,
        String message,
        String traceId,
        String spanId,
        String correlationId,
        @JsonProperty("logger_name") String logger,
        @JsonProperty("thread_name") String thread,
        String stack_trace
) {}
