package com.company.finance_api.ai.repository;

import com.company.finance_api.ai.domain.AiInteractionLogEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** AI etkileşim log kayıtları için Spring Data repository. */
public interface AiInteractionLogRepository extends JpaRepository<AiInteractionLogEntity, UUID> {}
