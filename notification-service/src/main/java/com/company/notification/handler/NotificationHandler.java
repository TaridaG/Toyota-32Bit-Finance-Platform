package com.company.notification.handler;

import com.company.notification.event.AlarmTriggeredEvent;

public interface NotificationHandler {
    void handle(AlarmTriggeredEvent event);
}
