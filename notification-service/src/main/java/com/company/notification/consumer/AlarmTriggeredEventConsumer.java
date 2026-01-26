package com.company.notification.consumer;

import com.company.notification.event.AlarmTriggeredEvent;
import com.company.notification.handler.NotificationHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AlarmTriggeredEventConsumer {

    private final NotificationHandler notificationHandler;

    public AlarmTriggeredEventConsumer(
            NotificationHandler notificationHandler
    ) {
        this.notificationHandler = notificationHandler;
    }

    @KafkaListener(
            topics = "alarm-triggered",
            groupId = "notification-service"
    )
    public void consume(AlarmTriggeredEvent event) {
        notificationHandler.handle(event);
    }
}
