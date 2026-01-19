package com.company.finance_api.alarm;

import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.enums.AlarmCondition;

import java.math.BigDecimal;

public class LessThanAlarmEvaluator implements AlarmEvaluator {

    @Override
    public AlarmCondition supports() {
        return AlarmCondition.LESS_THAN;
    }

    @Override
    public boolean isTriggered(AlarmRule rule, BigDecimal currentPrice) {
        return currentPrice.compareTo(rule.getThreshold()) < 0;
    }
}
