package com.company.reporting.application.impl;

import com.company.reporting.application.ReportRequestPublisher;
import com.company.reporting.event.ReportRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaReportRequestPublisher implements ReportRequestPublisher {

    public static final String TOPIC = "report.requested";

    private final KafkaTemplate<String, ReportRequestedEvent> kafkaTemplate;

    @Override
    public void publish(ReportRequestedEvent event) {
        String key = event.getReportId().toString();
        kafkaTemplate.send(TOPIC, key, event);
    }
}