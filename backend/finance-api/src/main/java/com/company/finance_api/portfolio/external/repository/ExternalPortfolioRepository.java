package com.company.finance_api.portfolio.external.repository;

import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalPortfolioRepository extends JpaRepository<ExternalPortfolio, Long> {

    List<ExternalPortfolio> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ExternalPortfolio> findByIdAndUserId(Long id, UUID userId);
}

