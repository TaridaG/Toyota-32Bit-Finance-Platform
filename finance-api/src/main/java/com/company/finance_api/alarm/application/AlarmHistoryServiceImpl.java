package com.company.finance_api.alarm.application;

import com.company.finance_api.alarm.infrastructure.http.dto.AlarmHistoryResponse;
import com.company.finance_api.alarm.infrastructure.persistence.AlarmHistoryRepository;
import com.company.finance_api.alarm.application.AlarmHistoryService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** AlarmHistoryServiceImpl iş mantığını uygular (alarm history service). */
@Service
@RequiredArgsConstructor
public class AlarmHistoryServiceImpl implements AlarmHistoryService {

  private final AlarmHistoryRepository repository;
  private final CurrentUserResolver currentUserResolver;

  /** MyTimeline sorgusunu döner. */
  @Override
  public List<AlarmHistoryResponse> getMyTimeline() {

    UUID userId = currentUserResolver.getCurrentUserId();

    return repository.findByUserIdOrderByTriggeredAtDesc(userId).stream()
        .filter(history -> !history.isDeleted())
        .map(
            history ->
                new AlarmHistoryResponse(
                    history.getInstrumentSymbol(),
                    history.getCondition().name(),
                    history.getPrice(),
                    history.getTriggeredAt()))
        .toList();
  }
}
