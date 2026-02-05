package com.company.finance_api.repository;

import com.company.finance_api.domain.AlarmHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AlarmHistoryRepository
        extends JpaRepository<AlarmHistory, Long> {

    List<AlarmHistory> findByUserIdOrderByTriggeredAtDesc(UUID userId);
}