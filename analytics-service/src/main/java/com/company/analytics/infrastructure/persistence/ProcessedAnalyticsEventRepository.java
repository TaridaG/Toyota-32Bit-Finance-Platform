package com.company.analytics.infrastructure.persistence;

import com.company.analytics.domain.ProcessedAnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedAnalyticsEventRepository
        extends JpaRepository<ProcessedAnalyticsEvent, Long> {

    Optional<ProcessedAnalyticsEvent> findByEventKey(String eventKey);

}