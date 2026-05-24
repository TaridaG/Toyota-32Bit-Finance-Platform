package com.company.notification.report.infrastructure.kafka;

import com.company.notification.bootstrap.config.kafka.KafkaTopicNames;
import com.company.notification.report.application.SendReportEmailUseCase;
import com.company.notification.report.infrastructure.kafka.messaging.ReportFailedMessage;
import com.company.notification.shared.kafka.KafkaCorrelationSupport;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * {@code report.failed} event'leri için Kafka inbound adapter.
 */
@Component
@RequiredArgsConstructor
public class ReportFailedEventConsumer {

    private final SendReportEmailUseCase sendReportEmailUseCase;

    /**
     * {@link SendReportEmailUseCase#handleFailed} metoduna devreder.
     */
    @KafkaListener(
            topics = KafkaTopicNames.REPORT_FAILED,
            groupId = "notification-service-report-failed",
            containerFactory = "reportFailedKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, ReportFailedMessage> record) {
        KafkaCorrelationSupport.runWithCorrelation(record, () ->
                sendReportEmailUseCase.handleFailed(record.value()));
    }
}
