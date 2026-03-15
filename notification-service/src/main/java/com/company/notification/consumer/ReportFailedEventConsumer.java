package com.company.notification.consumer;

import com.company.notification.event.ReportFailedEvent;
import com.company.notification.handler.ReportNotificationHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportFailedEventConsumer {

    private final ReportNotificationHandler reportNotificationHandler;

    @KafkaListener(
            topics = "report.failed",
            groupId = "notification-service-report-failed",
            containerFactory = "reportFailedKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, ReportFailedEvent> record) {
        try {
            Header correlationHeader = record.headers().lastHeader("correlationId");
            if (correlationHeader != null) {
                MDC.put("correlationId",
                        new String(correlationHeader.value(), StandardCharsets.UTF_8));
            }

            reportNotificationHandler.handleFailed(record.value());
        } finally {
            MDC.remove("correlationId");
        }
    }
}