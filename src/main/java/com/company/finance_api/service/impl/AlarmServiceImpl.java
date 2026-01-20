package com.company.finance_api.service.impl;

import com.company.finance_api.alarm.AlarmEvaluator;
import com.company.finance_api.alarm.AlarmEvaluatorFactory;
import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.repository.AlarmRuleRepository;
import com.company.finance_api.service.AlarmService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class AlarmServiceImpl implements AlarmService {

    private final AlarmRuleRepository alarmRuleRepository;
    private final AlarmEvaluatorFactory evaluatorFactory;

    public AlarmServiceImpl(AlarmRuleRepository alarmRuleRepository,AlarmEvaluatorFactory evaluatorFactory) {
        this.alarmRuleRepository = alarmRuleRepository;
        this.evaluatorFactory = evaluatorFactory;
    }

    @Override
    public List<AlarmRule> checkAlarms(
            Instrument instrument,
            InstrumentPrice latestPrice
    ) {

        List<AlarmRule> activeAlarms =
                alarmRuleRepository.findByInstrumentAndActiveTrue(instrument);

        List<AlarmRule> triggeredAlarms = new ArrayList<>();

        for (AlarmRule alarm : activeAlarms) {
            AlarmEvaluator evaluator =
                    evaluatorFactory.getEvaluator(alarm.getCondition());

            boolean triggered = evaluator.evaluate(alarm, latestPrice);

            if (triggered) {
                alarm.deactivate();
                triggeredAlarms.add(alarm);
            }
        }

        return triggeredAlarms;
    }

    private boolean isTriggered(AlarmRule alarm, BigDecimal currentPrice) {

        BigDecimal threshold = alarm.getThreshold();

        return switch (alarm.getCondition()) {
            case GREATER_THAN -> currentPrice.compareTo(threshold) > 0;
            case LESS_THAN -> currentPrice.compareTo(threshold) < 0;
            case EQUAL -> currentPrice.compareTo(threshold) == 0;
            case PERCENT_CHANGE_UP, PERCENT_CHANGE_DOWN ->
                    false; // % logic ileride eklenecek
        };
    }
}
