package com.company.logconsumer.intelligence.infrastructure.http;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SystemIntelligenceControllerTest {

    @Test
    void current_aggregatesCountersAndLag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Counter.builder("kafka_events_failed_total")
                .tag("service", "log-consumer-service")
                .register(registry)
                .increment(2);
        Counter.builder("kafka_events_skipped_total")
                .tag("service", "log-consumer-service")
                .register(registry)
                .increment(1);
        Gauge.builder("kafka.consumer.records.lag.max", () -> 42.0)
                .register(registry);

        SystemIntelligenceController controller = new SystemIntelligenceController(registry);

        Map<String, Object> response = controller.current();

        assertEquals(2.0, response.get("failedEventsTotal"));
        assertEquals(1.0, response.get("skippedEventsTotal"));
        assertEquals(0.0, response.get("dlqPublishedTotal"));
        assertEquals(42.0, response.get("kafkaLagMax"));
    }

    @Test
    void current_returnsNullLagWhenGaugeMissing() {
        SystemIntelligenceController controller =
                new SystemIntelligenceController(new SimpleMeterRegistry());

        Map<String, Object> response = controller.current();

        assertNull(response.get("kafkaLagMax"));
    }
}
