package com.company.finance_api.event.publisher;

import com.company.finance_api.event.AlarmTriggeredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({ "dev", "test" }) // prod'da otomatik devre dışı; test: Kafka yokken no-op log publisher
public class LogAlarmEventPublisher implements AlarmEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(LogAlarmEventPublisher.class);

    @Override
    public void publish(AlarmTriggeredEvent event) {
        log.info(
                "ALARM_EVENT alarmId={}, userId={}, instrument={}, condition={}, price={}, at={}",
                event.getAlarmId(),
                event.getUserId(),
                event.getInstrumentSymbol(),
                event.getCondition(),
                event.getPrice(),
                event.getTriggeredAt()
        );
    }
}
