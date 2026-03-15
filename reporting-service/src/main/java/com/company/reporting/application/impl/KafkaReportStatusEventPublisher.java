package com.company.reporting.application.impl;

import com.company.reporting.application.ReportStatusEventPublisher;
import com.company.reporting.event.ReportCompletedEvent;
import com.company.reporting.event.ReportFailedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaReportStatusEventPublisher implements ReportStatusEventPublisher {

    public static final String REPORT_COMPLETED_TOPIC = "report.completed";
    public static final String REPORT_FAILED_TOPIC = "report.failed";

    private final KafkaTemplate<String, ReportCompletedEvent> reportCompletedKafkaTemplate;
    private final KafkaTemplate<String, ReportFailedEvent> reportFailedKafkaTemplate;

    @Override
    public void publishCompleted(ReportCompletedEvent event) {
        reportCompletedKafkaTemplate.send(
                REPORT_COMPLETED_TOPIC,
                event.getReportId().toString(),
                event
        );
    }

    @Override
    public void publishFailed(ReportFailedEvent event) {
        reportFailedKafkaTemplate.send(
                REPORT_FAILED_TOPIC,
                event.getReportId().toString(),
                event
        );
    }
}