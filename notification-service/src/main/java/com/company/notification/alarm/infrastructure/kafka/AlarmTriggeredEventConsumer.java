package com.company.notification.alarm.infrastructure.kafka;

import com.company.notification.alarm.application.SendAlarmEmailUseCase;
import com.company.notification.alarm.infrastructure.kafka.messaging.AlarmTriggeredMessage;
import com.company.notification.bootstrap.config.kafka.KafkaTopicNames;
import com.company.notification.shared.kafka.KafkaCorrelationSupport;
import com.company.notification.shared.metrics.AlarmMetrics;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * {@code alarm-triggered} topic'i için Kafka inbound adapter.
 * Alarm metriklerini kaydeder ve {@link SendAlarmEmailUseCase}'e devreder.
 */
@Component
@RequiredArgsConstructor
public class AlarmTriggeredEventConsumer {

    private final SendAlarmEmailUseCase sendAlarmEmailUseCase;
    private final AlarmMetrics alarmMetrics;

    /**
     * Tek bir alarm-triggered kaydını işler: correlation id, metrikler ve e-posta teslimatı.
     */
    @KafkaListener(
            topics = KafkaTopicNames.ALARM_TRIGGERED,
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, AlarmTriggeredMessage> record) {
        KafkaCorrelationSupport.runWithCorrelation(record, () -> {
            alarmMetrics.incrementTriggered();
            sendAlarmEmailUseCase.handle(record.value());
        });
    }
}
