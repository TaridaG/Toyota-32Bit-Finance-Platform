package com.company.logconsumer.intelligence.infrastructure.http;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal operasyon endpoint'i: Kafka lag, failed/skipped/DLQ metrik özetleri.
 */
@RestController
@RequestMapping("/internal/system-intelligence")
@RequiredArgsConstructor
public class SystemIntelligenceController {

    private final MeterRegistry meterRegistry;

    @GetMapping
    public Map<String, Object> current() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("kafkaLagMax", resolveKafkaLagMax());
        response.put("failedEventsTotal", sumCounters("kafka_events_failed_total"));
        response.put("skippedEventsTotal", sumCounters("kafka_events_skipped_total"));
        response.put("dlqPublishedTotal", sumCounters("kafka_events_dlq_published_total"));
        return response;
    }

    private double sumCounters(String metricName) {
        return meterRegistry.find(metricName)
                .counters()
                .stream()
                .mapToDouble(Counter::count)
                .sum();
    }

    private Double resolveKafkaLagMax() {
        List<String> lagMetricCandidates = List.of(
                "kafka.consumer.records.lag.max",
                "kafka_consumer_records_lag_max"
        );
        for (String candidate : lagMetricCandidates) {
            Gauge gauge = meterRegistry.find(candidate).gauge();
            if (gauge != null && !Double.isNaN(gauge.value())) {
                return gauge.value();
            }
        }
        return null;
    }
}
