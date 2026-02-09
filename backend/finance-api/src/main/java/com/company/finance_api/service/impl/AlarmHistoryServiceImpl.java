package com.company.finance_api.service.impl;

import com.company.finance_api.dto.AlarmHistoryResponse;
import com.company.finance_api.repository.AlarmHistoryRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.AlarmHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlarmHistoryServiceImpl implements AlarmHistoryService {

    private final AlarmHistoryRepository repository;
    private final CurrentUserResolver currentUserResolver;

    @Override
    public List<AlarmHistoryResponse> getMyTimeline() {

        UUID userId = currentUserResolver.getCurrentUserId();

        return repository.findByUserIdOrderByTriggeredAtDesc(userId)
                .stream()
                .map(history -> new AlarmHistoryResponse(
                        history.getInstrumentSymbol(),
                        history.getCondition().name(),
                        history.getPrice(),
                        history.getTriggeredAt()
                ))
                .toList();
    }
}