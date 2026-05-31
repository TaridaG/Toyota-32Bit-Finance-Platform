package com.company.finance_api.outbox.infrastructure.persistence;

import com.company.finance_api.outbox.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** ProcessedEvent entity persistence için Spring Data repository. */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {}
