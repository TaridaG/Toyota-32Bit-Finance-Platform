package com.company.finance_api.alarm;

import com.company.finance_api.alarm.domain.AlarmRule;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.AlarmCondition;
import org.springframework.stereotype.Component;

/** Fiyat eşiği aştığında tetiklenen alarm evaluator. */
@Component
public class GreaterThanAlarmEvaluator implements AlarmEvaluator {

  @Override
  public AlarmCondition supports() {
    return AlarmCondition.GREATER_THAN;
  }

  @Override
  public boolean evaluate(AlarmRule alarm, InstrumentPrice latestPrice) {
    return latestPrice.getPrice().compareTo(alarm.getThreshold()) > 0;
  }
}
