package com.company.notification.consumer;

import com.company.notification.event.AlarmTriggeredEvent;
import com.company.notification.handler.NotificationHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Component
public class AlarmTriggeredEventConsumer {

    private final NotificationHandler notificationHandler;

    public AlarmTriggeredEventConsumer(NotificationHandler notificationHandler) {
        this.notificationHandler = notificationHandler;
    }

    @KafkaListener(
            topics = "alarm-triggered",
            groupId = "notification-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, AlarmTriggeredEvent> record) {
        try {
            Header correlationHeader =
                    record.headers().lastHeader("correlationId");
            if (correlationHeader != null) {
                String correlationId =
                        new String(correlationHeader.value(), StandardCharsets.UTF_8);
                MDC.put("correlationId", correlationId);
            }
            notificationHandler.handle(record.value());
        } finally {
            MDC.remove("correlationId");
        }
    }
}
