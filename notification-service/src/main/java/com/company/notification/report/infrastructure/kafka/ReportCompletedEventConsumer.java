package com.company.notification.report.infrastructure.kafka;

import com.company.notification.bootstrap.config.kafka.KafkaTopicNames;
import com.company.notification.report.application.SendReportEmailUseCase;
import com.company.notification.report.infrastructure.kafka.messaging.ReportCompletedMessage;
import com.company.notification.shared.kafka.KafkaCorrelationSupport;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * {@code report.completed} event'leri için Kafka inbound adapter.
 */
@Component
@RequiredArgsConstructor
public class ReportCompletedEventConsumer {

    private final SendReportEmailUseCase sendReportEmailUseCase;

    /**
     * {@link SendReportEmailUseCase#handleCompleted} metoduna devreder.
     */
    @KafkaListener(
            topics = KafkaTopicNames.REPORT_COMPLETED,
            groupId = "notification-service-report-completed",
            containerFactory = "reportCompletedKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, ReportCompletedMessage> record) {
        KafkaCorrelationSupport.runWithCorrelation(record, () ->
                sendReportEmailUseCase.handleCompleted(record.value()));
    }
}
