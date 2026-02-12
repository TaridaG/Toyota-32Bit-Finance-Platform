package com.company.finance_api.alarm;

import com.company.finance_api.alarm.AlarmEvaluator;
import com.company.finance_api.alarm.AlarmEvaluatorFactory;
import com.company.finance_api.domain.*;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.event.publisher.AlarmEventPublisher;
import com.company.finance_api.repository.*;
import com.company.finance_api.service.impl.AlarmServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlarmServiceImplTest {

    @Mock AlarmRuleRepository alarmRuleRepository;
    @Mock AlarmEvaluatorFactory evaluatorFactory;
    @Mock AlarmEventPublisher alarmEventPublisher;
    @Mock InstrumentRepository instrumentRepository;
    @Mock UserRepository userRepository;

    @InjectMocks
    AlarmServiceImpl alarmService;

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

        InstrumentPrice price = new InstrumentPrice(
                instrument,
                com.company.finance_api.domain.enums.PriceType.MARKET,
                BigDecimal.valueOf(200),
                Instant.now()
        );

        AlarmEvaluator evaluator = mock(AlarmEvaluator.class);

        when(alarmRuleRepository.findByInstrumentAndActiveTrue(instrument))
                .thenReturn(List.of(rule));

        when(evaluatorFactory.getEvaluator(AlarmCondition.GREATER_THAN))
                .thenReturn(evaluator);

        when(evaluator.evaluate(rule, price))
                .thenReturn(true);

        List<AlarmRule> result =
                alarmService.checkAlarms(instrument, price);

        assertEquals(1, result.size());

        verify(rule).deactivate();
        verify(alarmRuleRepository).save(rule);
        verify(alarmEventPublisher).publish(any());
    }
}
