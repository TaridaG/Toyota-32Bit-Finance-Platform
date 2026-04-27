package com.company.notification.repository;

import com.company.notification.domain.PendingInsightEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PendingInsightEventRepository extends JpaRepository<PendingInsightEvent, Long> {

    List<PendingInsightEvent> findByProcessedFalse();

    List<PendingInsightEvent> findByProcessedFalseAndUserId(UUID userId);
}
