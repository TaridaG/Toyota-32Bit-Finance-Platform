package com.company.notification.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Alarm bildirim throughput'u için Micrometer counter'ları.
 */
@Component
public class AlarmMetrics {

    private final Counter alarmTriggeredCounter;

    public AlarmMetrics(MeterRegistry meterRegistry) {
        this.alarmTriggeredCounter =
                Counter.builder("alarm_triggered_total")
                        .description("Total triggered alarms")
                        .register(meterRegistry);
    }

    /** Consume edilen bir alarm-triggered event'i kaydeder. */
    public void incrementTriggered() {
        alarmTriggeredCounter.increment();
    }
}