package com.company.finance_api.alarm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.company.finance_api.domain.AlarmRule;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.domain.enums.PriceType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EqualAlarmEvaluatorTest {

  private final EqualAlarmEvaluator evaluator = new EqualAlarmEvaluator();

  @Test
  void shouldTrigger_whenPriceMatchesThresholdExactly() {
    AlarmRule rule = rule(new BigDecimal("99.500000"));
    InstrumentPrice price = price(new BigDecimal("99.500000"));

    assertTrue(evaluator.evaluate(rule, price));
  }

  @Test
  void shouldNotTrigger_whenPriceDiffersFromThreshold() {
    AlarmRule rule = rule(BigDecimal.valueOf(100));
    InstrumentPrice price = price(BigDecimal.valueOf(100.01));

    assertFalse(evaluator.evaluate(rule, price));
  }

  private static AlarmRule rule(BigDecimal threshold) {
    return new AlarmRule(mock(User.class), mock(Instrument.class), AlarmCondition.EQUAL, threshold);
  }

  private static InstrumentPrice price(BigDecimal value) {
    return new InstrumentPrice(
        mock(Instrument.class), PriceType.MARKET, value, Instant.parse("2026-05-24T12:00:00Z"));
  }
}
