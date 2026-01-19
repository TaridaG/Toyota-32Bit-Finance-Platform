package com.company.finance_api.alarm;

import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.enums.AlarmCondition;

import java.math.BigDecimal;

public interface AlarmEvaluator {

    AlarmCondition supports();
    /**
     * Alarm tetiklendimi
     * @param rule alarm kuralı
     * @param currentPrice güncel fiyat
     * @return tetiklendiyse true
     */
    boolean isTriggered(AlarmRule rule, BigDecimal currentPrice);

}
