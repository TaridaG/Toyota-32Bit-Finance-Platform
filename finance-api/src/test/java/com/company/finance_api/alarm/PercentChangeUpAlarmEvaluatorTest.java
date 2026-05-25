package com.company.finance_api.alarm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.finance_api.alarm.domain.AlarmRule;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.repository.InstrumentPriceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PercentChangeUpAlarmEvaluatorTest {

  @Mock InstrumentPriceRepository priceRepository;

  PercentChangeUpAlarmEvaluator evaluator;

  Instrument instrument;

  @BeforeEach
  void setUp() {
    evaluator = new PercentChangeUpAlarmEvaluator(priceRepository);
    instrument = mock(Instrument.class);
  }

  @Test
  void shouldTrigger_whenPercentIncreaseMeetsThreshold() {
    Instant currentTs = Instant.parse("2026-05-24T12:00:00Z");
    InstrumentPrice current =
        new InstrumentPrice(instrument, PriceType.MARKET, BigDecimal.valueOf(120), currentTs);
    InstrumentPrice previous =
        new InstrumentPrice(
            instrument, PriceType.MARKET, BigDecimal.valueOf(100), currentTs.minusSeconds(60));
    AlarmRule rule =
        new AlarmRule(
            mock(User.class), instrument, AlarmCondition.PERCENT_CHANGE_UP, BigDecimal.valueOf(15));

    when(priceRepository.findFirstByInstrumentAndPriceTypeAndTimestampLessThanOrderByTimestampDesc(
            instrument, PriceType.MARKET, currentTs))
        .thenReturn(Optional.of(previous));

    assertTrue(evaluator.evaluate(rule, current));
  }

  @Test
  void shouldNotTrigger_whenNoPreviousPriceExists() {
    Instant currentTs = Instant.parse("2026-05-24T12:00:00Z");
    InstrumentPrice current =
        new InstrumentPrice(instrument, PriceType.MARKET, BigDecimal.valueOf(120), currentTs);
    AlarmRule rule =
        new AlarmRule(
            mock(User.class), instrument, AlarmCondition.PERCENT_CHANGE_UP, BigDecimal.valueOf(5));

    when(priceRepository.findFirstByInstrumentAndPriceTypeAndTimestampLessThanOrderByTimestampDesc(
            instrument, PriceType.MARKET, currentTs))
        .thenReturn(Optional.empty());

    assertFalse(evaluator.evaluate(rule, current));
  }

  @Test
  void shouldNotTrigger_whenPreviousPriceIsZero() {
    Instant currentTs = Instant.parse("2026-05-24T12:00:00Z");
    InstrumentPrice current =
        new InstrumentPrice(instrument, PriceType.MARKET, BigDecimal.valueOf(120), currentTs);
    InstrumentPrice previous =
        new InstrumentPrice(
            instrument, PriceType.MARKET, BigDecimal.ZERO, currentTs.minusSeconds(60));
    AlarmRule rule =
        new AlarmRule(
            mock(User.class), instrument, AlarmCondition.PERCENT_CHANGE_UP, BigDecimal.valueOf(5));

    when(priceRepository.findFirstByInstrumentAndPriceTypeAndTimestampLessThanOrderByTimestampDesc(
            instrument, PriceType.MARKET, currentTs))
        .thenReturn(Optional.of(previous));

    assertFalse(evaluator.evaluate(rule, current));
  }

  @Test
  void shouldNotTrigger_whenIncreaseIsBelowThreshold() {
    Instant currentTs = Instant.parse("2026-05-24T12:00:00Z");
    InstrumentPrice current =
        new InstrumentPrice(instrument, PriceType.MARKET, BigDecimal.valueOf(105), currentTs);
    InstrumentPrice previous =
        new InstrumentPrice(
            instrument, PriceType.MARKET, BigDecimal.valueOf(100), currentTs.minusSeconds(60));
    AlarmRule rule =
        new AlarmRule(
            mock(User.class), instrument, AlarmCondition.PERCENT_CHANGE_UP, BigDecimal.valueOf(10));

    when(priceRepository.findFirstByInstrumentAndPriceTypeAndTimestampLessThanOrderByTimestampDesc(
            instrument, PriceType.MARKET, currentTs))
        .thenReturn(Optional.of(previous));

    assertFalse(evaluator.evaluate(rule, current));
  }
}
