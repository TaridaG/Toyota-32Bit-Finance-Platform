package com.company.notification.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class AlarmMetrics {

    private final Counter alarmTriggeredCounter;

    public AlarmMetrics(MeterRegistry meterRegistry) {
        this.alarmTriggeredCounter =
                Counter.builder("alarm_triggered_total")
                        .description("Total triggered alarms")
                        .register(meterRegistry);
    }

    public void incrementTriggered() {
        alarmTriggeredCounter.increment();
    }
}