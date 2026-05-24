package com.company.notification.alarm.infrastructure.kafka;

import com.company.notification.alarm.application.SendAlarmEmailUseCase;
import com.company.notification.alarm.infrastructure.kafka.messaging.AlarmTriggeredMessage;
import com.company.notification.shared.metrics.AlarmMetrics;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AlarmTriggeredEventConsumerTest {

    @Mock
    private SendAlarmEmailUseCase sendAlarmEmailUseCase;
    @Mock
    private AlarmMetrics alarmMetrics;

    @InjectMocks
    private AlarmTriggeredEventConsumer consumer;

    @Test
    void consume_increments_metrics_and_delegates() {
        AlarmTriggeredMessage message = new AlarmTriggeredMessage();
        message.setUserEmail("user@example.com");
        ConsumerRecord<String, AlarmTriggeredMessage> record =
                new ConsumerRecord<>("alarm-triggered", 0, 0L, "key", message);

        consumer.consume(record);

        verify(alarmMetrics).incrementTriggered();
        verify(sendAlarmEmailUseCase).handle(message);
    }
}
