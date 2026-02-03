package com.company.finance_api.event.listener;

import com.company.finance_api.alarm.AlarmEvaluator;
import com.company.finance_api.alarm.AlarmEvaluatorFactory;
import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.event.AlarmTriggeredEvent;
import com.company.finance_api.event.PriceUpdatedEvent;
import com.company.finance_api.event.publisher.AlarmEventPublisher;
import com.company.finance_api.repository.AlarmRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PriceUpdatedEventListener {

    private final AlarmRuleRepository alarmRuleRepository;
    private final AlarmEvaluatorFactory evaluatorFactory;
    private final AlarmEventPublisher alarmEventPublisher;

    @EventListener
    public void handle(PriceUpdatedEvent event) {

        InstrumentPrice price = event.price();

        List<AlarmRule> rules =
                alarmRuleRepository.findByInstrumentAndActiveTrue(
                        price.getInstrument()
                );

        for (AlarmRule rule : rules) {

            AlarmEvaluator evaluator =
                    evaluatorFactory.getEvaluator(rule.getCondition());

            boolean triggered =
                    evaluator.evaluate(rule, price);

            if (triggered) {
                alarmEventPublisher.publish(
                        AlarmTriggeredEvent.of(
                                rule.getId(),                             // alarmId
                                rule.getUser().getId(),                   // userId (UUID)
                                price.getInstrument().getSymbol(),        // instrumentSymbol
                                rule.getCondition(),                      // AlarmCondition
                                price.getPrice()                          // BigDecimal
                        )
                );
            }
        }
    }
}