package com.company.finance_api.repository;

import com.company.finance_api.domain.InfoCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InfoCardRepository extends JpaRepository<InfoCardEntity, UUID>, JpaSpecificationExecutor<InfoCardEntity> {

    Optional<InfoCardEntity> findBySlug(String slug);

    List<InfoCardEntity> findByStatusOrderByUpdatedAtDesc(String status);

    long countByStatus(String status);
}
