package com.company.finance_api.ai.repository;

import com.company.finance_api.ai.domain.AiInteractionLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiInteractionLogRepository extends JpaRepository<AiInteractionLogEntity, UUID> {
}
