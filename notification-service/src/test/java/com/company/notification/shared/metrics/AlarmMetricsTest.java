package com.company.notification.shared.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlarmMetricsTest {

    @Test
    void incrementTriggered_records_counter() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        AlarmMetrics metrics = new AlarmMetrics(registry);

        metrics.incrementTriggered();
        metrics.incrementTriggered();

        assertEquals(2.0, registry.get("alarm_triggered_total").counter().count());
    }
}
