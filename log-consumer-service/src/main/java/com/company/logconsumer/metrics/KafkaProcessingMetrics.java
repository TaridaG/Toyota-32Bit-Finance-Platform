package com.company.logconsumer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class KafkaProcessingMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter processedCounter;
    private final Counter failureCounter;
    private final Timer processingTimer;

    public KafkaProcessingMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.processedCounter =
                Counter.builder("kafka_events_processed_total")
                        .description("Total processed Kafka events")
                        .register(meterRegistry);

        this.failureCounter =
                Counter.builder("kafka_events_failed_total")
                        .description("Total failed Kafka events")
                        .register(meterRegistry);

        this.processingTimer =
                Timer.builder("kafka_event_processing_latency")
                        .description("Kafka event processing latency")
                        .register(meterRegistry);
    }

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordSuccess(Timer.Sample sample) {
        processedCounter.increment();
        sample.stop(processingTimer);
    }

    public void recordFailure(Timer.Sample sample) {
        failureCounter.increment();
        sample.stop(processingTimer);
    }
}