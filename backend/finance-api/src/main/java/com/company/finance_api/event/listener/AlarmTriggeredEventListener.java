package com.company.finance_api.event.listener;

import com.company.finance_api.event.AlarmTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AlarmTriggeredEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(AlarmTriggeredEventListener.class);

    @EventListener
    public void handleAlarmTriggered(AlarmTriggeredEvent event) {

        // ŞİMDİLİK SADECE LOG
        log.info(
                "EVENT_RECEIVED alarmId={}, userId={}, instrument={}, condition={}, price={}, at={}",
                event.getAlarmId(),
                event.getUserId(),
                event.getInstrumentSymbol(),
                event.getCondition(),
                event.getPrice(),
                event.getTriggeredAt()
        );
    }
}
