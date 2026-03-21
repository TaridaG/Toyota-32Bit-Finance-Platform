package com.company.finance_api.repository;

import com.company.finance_api.domain.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findByUserIdOrderByCreatedAtAsc(UUID userId);
}