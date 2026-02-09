package com.company.finance_api.event.listener;

import com.company.finance_api.domain.AlarmHistory;
import com.company.finance_api.event.AlarmTriggeredEvent;
import com.company.finance_api.repository.AlarmHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlarmTriggeredEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(AlarmTriggeredEventListener.class);
    private final AlarmHistoryRepository alarmHistoryRepository;


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
        alarmHistoryRepository.save(
                new AlarmHistory(
                        event.getUserId(),
                        event.getInstrumentSymbol(),
                        event.getCondition(),
                        event.getPrice()
                )
        );
    }
}
