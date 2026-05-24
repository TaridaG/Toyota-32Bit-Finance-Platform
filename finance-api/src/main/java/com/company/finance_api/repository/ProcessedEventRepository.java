package com.company.finance_api.repository;

import com.company.finance_api.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

/** ProcessedEvent entity persistence için Spring Data repository. */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {}
