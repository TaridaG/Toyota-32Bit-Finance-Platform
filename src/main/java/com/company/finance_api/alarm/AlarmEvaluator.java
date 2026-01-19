package com.company.finance_api.alarm;

import com.company.finance_api.domain.AlarmRule;

import java.math.BigDecimal;

public interface AlarmEvaluator {


    /**
     * Alarm tetiklendimi
     * @param rule alarm kuralı
     * @param currentPrice güncel fiyat
     * @return tetiklendiyse true
     */
    boolean isTriggered(AlarmRule rule, BigDecimal currentPrice);

}
