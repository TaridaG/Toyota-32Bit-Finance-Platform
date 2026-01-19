package com.company.finance_api.alarm;

import com.company.finance_api.domain.AlarmRule;

import java.math.BigDecimal;

public class GreaterThanAlarmEvaluator implements AlarmEvaluator {



    @Override
    public boolean isTriggered(AlarmRule rule, BigDecimal currentPrice) {
        return currentPrice.compareTo(rule.getThreshold()) > 0;
    }
}
