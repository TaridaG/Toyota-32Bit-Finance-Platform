/**
 * Platform genelinde merkezi loglama: Logstash JSON, {@code app.logs} Kafka topic'i ve MDC alanları.
 * <p>
 * Uygulama servisleri bu modüle bağımlı olur; {@code logback-spring.xml} içinde
 * {@code logback-kafka-base.xml} include edilir. {@code log-consumer-service} Kafka'dan okuyup
 * OpenSearch'e yazar.
 */
package com.company.platform.logging;
