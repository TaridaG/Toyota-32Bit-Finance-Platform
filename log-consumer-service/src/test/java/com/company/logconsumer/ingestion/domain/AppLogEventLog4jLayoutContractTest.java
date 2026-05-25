package com.company.logconsumer.ingestion.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies Kafka payloads produced by Log4j2 {@code JsonTemplateLayout} deserialize into {@link AppLogEvent}.
 */
class AppLogEventLog4jLayoutContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserialize_log4jJsonTemplateLayoutFields() throws Exception {
        String json = """
                {
                  "@timestamp": "2026-05-25T12:00:00.000Z",
                  "level": "INFO",
                  "message": "pipeline contract ok",
                  "logger_name": "com.company.finance_api.bootstrap.FinanceApiApplication",
                  "thread_name": "main",
                  "service": "finance-api",
                  "traceId": "abc123",
                  "correlationId": "corr-9"
                }
                """;

        AppLogEvent event = objectMapper.readValue(json, AppLogEvent.class);

        assertEquals("2026-05-25T12:00:00.000Z", event.timestamp());
        assertEquals("INFO", event.level());
        assertEquals("finance-api", event.serviceName());
        assertEquals("pipeline contract ok", event.message());
        assertEquals("com.company.finance_api.bootstrap.FinanceApiApplication", event.logger());
        assertEquals("main", event.thread());
        assertEquals("abc123", event.traceId());
        assertEquals("corr-9", event.correlationId());
    }

    @Test
    void deserialize_legacyServiceNameAlias() throws Exception {
        String json = """
                {
                  "timestamp": "2026-05-25T12:00:00Z",
                  "level": "WARN",
                  "serviceName": "market-data-service",
                  "message": "legacy field"
                }
                """;

        AppLogEvent event = objectMapper.readValue(json, AppLogEvent.class);

        assertNotNull(event);
        assertEquals("market-data-service", event.serviceName());
        assertEquals("WARN", event.level());
    }
}
