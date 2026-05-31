package com.company.finance_api.alarm.domain.evaluator;

import com.company.finance_api.alarm.domain.AlarmRule;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.AlarmCondition;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Fiyat eşik değere eşit olduğunda tetiklenen alarm evaluator. */
@Component
public class EqualAlarmEvaluator implements AlarmEvaluator {

  @Override
  public AlarmCondition supports() {
    return AlarmCondition.EQUAL;
  }

  @Override
  public boolean evaluate(AlarmRule rule, InstrumentPrice price) {
    BigDecimal threshold = rule.getThreshold();
    BigDecimal current = price.getPrice();

    return current.compareTo(threshold) == 0;
  }
}
