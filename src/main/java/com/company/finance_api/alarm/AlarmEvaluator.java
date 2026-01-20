package com.company.finance_api.alarm;

import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.AlarmCondition;

import java.math.BigDecimal;

public interface AlarmEvaluator {

    AlarmCondition supports();

    boolean evaluate(AlarmRule alarm, InstrumentPrice latestPrice);

}
