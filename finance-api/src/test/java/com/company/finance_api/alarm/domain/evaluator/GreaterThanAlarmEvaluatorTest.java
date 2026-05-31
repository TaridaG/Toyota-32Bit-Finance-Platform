package com.company.finance_api.alarm.domain.evaluator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.company.finance_api.alarm.domain.AlarmRule;
import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.alarm.domain.enums.AlarmCondition;
import com.company.finance_api.pricing.domain.enums.PriceType;
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
