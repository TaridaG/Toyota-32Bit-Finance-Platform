package com.company.finance_api.alarm.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.company.finance_api.alarm.AlarmEvaluator;
import com.company.finance_api.alarm.AlarmEvaluatorFactory;
import com.company.finance_api.alarm.domain.AlarmRule;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmHistoryRepository;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmRuleRepository;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.event.publisher.AlarmEventPublisher;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlarmServiceImplTest {

  @Mock AlarmRuleRepository alarmRuleRepository;
  @Mock AlarmHistoryRepository alarmHistoryRepository;
  @Mock AlarmEvaluatorFactory evaluatorFactory;
  @Mock AlarmEventPublisher alarmEventPublisher;
  @Mock InstrumentRepository instrumentRepository;
  @Mock UserRepository userRepository;

  @InjectMocks AlarmServiceImpl alarmService;

  @Test
  void checkAlarms_shouldDeactivateAndPublish_whenTriggered() {

    User user = mock(User.class);
    when(user.getId()).thenReturn(UUID.randomUUID());

    Instrument instrument = mock(Instrument.class);
    when(instrument.getSymbol()).thenReturn("BTCUSDT");

    AlarmRule rule = mock(AlarmRule.class);
    when(rule.getCondition()).thenReturn(AlarmCondition.GREATER_THAN);
    when(rule.getUser()).thenReturn(user);
    when(rule.getInstrument()).thenReturn(instrument);

    InstrumentPrice price =
        new InstrumentPrice(
            instrument,
            com.company.finance_api.domain.enums.PriceType.MARKET,
            BigDecimal.valueOf(200),
            Instant.now());

    AlarmEvaluator evaluator = mock(AlarmEvaluator.class);

    when(alarmRuleRepository.findByInstrumentAndActiveTrue(instrument)).thenReturn(List.of(rule));

    when(evaluatorFactory.getEvaluator(AlarmCondition.GREATER_THAN))
        .thenReturn(Optional.of(evaluator));

    when(evaluator.evaluate(rule, price)).thenReturn(true);

    List<AlarmRule> result = alarmService.checkAlarms(instrument, price);

    assertEquals(1, result.size());

    verify(rule).deactivate();
    verify(alarmRuleRepository).save(rule);
    verify(alarmEventPublisher).publish(any());
  }
}
