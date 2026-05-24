package com.company.notification.report.infrastructure.kafka;

import com.company.notification.report.application.SendReportEmailUseCase;
import com.company.notification.report.infrastructure.kafka.messaging.ReportFailedMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportFailedEventConsumerTest {

    @Mock
    private SendReportEmailUseCase sendReportEmailUseCase;

    @InjectMocks
    private ReportFailedEventConsumer consumer;

    @Test
    void consume_delegates_to_use_case() {
        ReportFailedMessage message = new ReportFailedMessage();
        message.setReportId(UUID.randomUUID());
        message.setUserEmail("user@example.com");
        message.setReason("timeout");
        ConsumerRecord<String, ReportFailedMessage> record =
                new ConsumerRecord<>("report.failed", 0, 0L, "key", message);

        consumer.consume(record);

        verify(sendReportEmailUseCase).handleFailed(message);
    }
}
