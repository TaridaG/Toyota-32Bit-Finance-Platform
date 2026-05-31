package com.company.finance_api.alarm.domain.evaluator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.company.finance_api.alarm.domain.AlarmRule;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.domain.enums.PriceType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GreaterThanAlarmEvaluatorTest {

  @Test
  void shouldTrigger_whenPriceIsGreaterThanThreshold() {

    // Mock entity'ler
    User user = mock(User.class);
    Instrument instrument = mock(Instrument.class);

    AlarmRule rule =
        new AlarmRule(user, instrument, AlarmCondition.GREATER_THAN, BigDecimal.valueOf(100));

    InstrumentPrice price =
        new InstrumentPrice(instrument, PriceType.MARKET, BigDecimal.valueOf(150), Instant.now());

    GreaterThanAlarmEvaluator evaluator = new GreaterThanAlarmEvaluator();

    boolean result = evaluator.evaluate(rule, price);

    assertTrue(result);
  }
}
