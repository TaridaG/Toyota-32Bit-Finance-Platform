package com.company.finance_api.alarm.domain.evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.company.finance_api.alarm.domain.enums.AlarmCondition;
import com.company.finance_api.pricing.infrastructure.persistence.InstrumentPriceRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlarmEvaluatorFactoryTest {

  AlarmEvaluatorFactory factory;

  @BeforeEach
  void setUp() {
    InstrumentPriceRepository priceRepository = mock(InstrumentPriceRepository.class);
    factory =
        new AlarmEvaluatorFactory(
            List.of(
                new GreaterThanAlarmEvaluator(),
                new LessThanAlarmEvaluator(),
                new EqualAlarmEvaluator(),
                new PercentChangeUpAlarmEvaluator(priceRepository),
                new PercentChangeDownAlarmEvaluator(priceRepository)));
  }

  @Test
  void getEvaluator_should_mapGreaterThan() {
    assertEvaluator(AlarmCondition.GREATER_THAN, GreaterThanAlarmEvaluator.class);
  }

  @Test
  void getEvaluator_should_mapLessThan() {
    assertEvaluator(AlarmCondition.LESS_THAN, LessThanAlarmEvaluator.class);
  }

  @Test
  void getEvaluator_should_mapEqual() {
    assertEvaluator(AlarmCondition.EQUAL, EqualAlarmEvaluator.class);
  }

  @Test
  void getEvaluator_should_mapPercentChangeUp() {
    assertEvaluator(AlarmCondition.PERCENT_CHANGE_UP, PercentChangeUpAlarmEvaluator.class);
  }

  @Test
  void getEvaluator_should_mapPercentChangeDown() {
    assertEvaluator(AlarmCondition.PERCENT_CHANGE_DOWN, PercentChangeDownAlarmEvaluator.class);
  }

  private void assertEvaluator(AlarmCondition condition, Class<? extends AlarmEvaluator> type) {
    var evaluator = factory.getEvaluator(condition);

    assertTrue(evaluator.isPresent());
    assertEquals(type, evaluator.get().getClass());
    assertEquals(condition, evaluator.get().supports());
  }
}
