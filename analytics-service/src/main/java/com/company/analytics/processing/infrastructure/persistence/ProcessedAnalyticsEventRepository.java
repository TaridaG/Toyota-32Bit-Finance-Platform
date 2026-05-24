package com.company.analytics.processing.infrastructure.persistence;

import com.company.analytics.processing.domain.ProcessedAnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** {@link ProcessedAnalyticsEvent} entity'si için Spring Data JPA repository. */
public interface ProcessedAnalyticsEventRepository
        extends JpaRepository<ProcessedAnalyticsEvent, Long> {

    /** Event key'e göre daha önce işlenmiş event kaydını döner. */
    Optional<ProcessedAnalyticsEvent> findByEventKey(String eventKey);

}
