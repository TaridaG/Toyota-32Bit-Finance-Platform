package com.company.finance_api.event.listener;

import com.company.finance_api.alarm.AlarmEvaluator;
import com.company.finance_api.alarm.AlarmEvaluatorFactory;
import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.event.AlarmTriggeredEvent;
import com.company.finance_api.event.PriceUpdatedEvent;
import com.company.finance_api.event.publisher.AlarmEventPublisher;
import com.company.finance_api.repository.AlarmRuleRepository;
import com.company.finance_api.service.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PriceUpdatedEventListener {

    private final AlarmService alarmService;

    @EventListener
    public void handle(PriceUpdatedEvent event) {
        alarmService.checkAlarms(
                event.price().getInstrument(),
                event.price()
        );
    }
}
