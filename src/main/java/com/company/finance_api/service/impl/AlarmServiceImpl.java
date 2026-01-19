package com.company.finance_api.service.impl;

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

    public AlarmServiceImpl(AlarmRuleRepository alarmRuleRepository) {
        this.alarmRuleRepository = alarmRuleRepository;
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
            if (isTriggered(alarm, latestPrice.getPrice())) {
                alarm.deactivate();               // tek seferlik alarm
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
