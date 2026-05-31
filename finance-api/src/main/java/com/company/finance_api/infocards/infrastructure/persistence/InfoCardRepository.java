package com.company.finance_api.infocards.infrastructure.persistence;

import com.company.finance_api.infocards.domain.InfoCardEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** InfoCard entity persistence için Spring Data repository. */
public interface InfoCardRepository
    extends JpaRepository<InfoCardEntity, UUID>, JpaSpecificationExecutor<InfoCardEntity> {

  Optional<InfoCardEntity> findBySlug(String slug);

  List<InfoCardEntity> findByStatusOrderByUpdatedAtDesc(String status);

  long countByStatus(String status);
}
