package com.company.finance_api.alarm.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.company.finance_api.alarm.domain.AlarmHistory;
import com.company.finance_api.alarm.domain.enums.AlarmCondition;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmHistoryRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlarmHistoryServiceImplTest {

  @Mock private AlarmHistoryRepository repository;
  @Mock private CurrentUserResolver currentUserResolver;

  @InjectMocks private AlarmHistoryServiceImpl alarmHistoryService;

  @Test
  void getMyTimeline_excludesDeletedAndMapsFields() {
    UUID userId = UUID.randomUUID();
    AlarmHistory active =
        new AlarmHistory(userId, "BTCUSDT", AlarmCondition.GREATER_THAN, BigDecimal.TEN, BigDecimal.valueOf(51000));
    AlarmHistory deleted =
        new AlarmHistory(userId, "ETHUSDT", AlarmCondition.LESS_THAN, BigDecimal.ONE, BigDecimal.TWO);
    deleted.markDeleted();

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(repository.findByUserIdOrderByTriggeredAtDesc(userId))
        .thenReturn(List.of(active, deleted));

    var timeline = alarmHistoryService.getMyTimeline();

    assertEquals(1, timeline.size());
    assertEquals("BTCUSDT", timeline.get(0).instrumentSymbol());
    assertEquals("GREATER_THAN", timeline.get(0).condition());
    assertTrue(timeline.get(0).price().compareTo(BigDecimal.valueOf(51000)) == 0);
  }
}
