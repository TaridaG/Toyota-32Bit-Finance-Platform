package com.company.notification.handler;

import com.company.notification.event.AlarmTriggeredEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LogNotificationHandler implements NotificationHandler {

    private static final Logger log =
            LoggerFactory.getLogger(LogNotificationHandler.class);

    @Override
    public void handle(AlarmTriggeredEvent event) {
        log.info(
                "NOTIFICATION alarmId={}, userId={}, instrument={}, condition={}, price={}",
                event.getAlarmId(),
                event.getUserId(),
                event.getInstrumentSymbol(),
                event.getCondition(),
                event.getPrice()
        );
    }
}
