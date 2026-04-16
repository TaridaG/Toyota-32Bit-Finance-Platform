package com.company.finance_api.portfolio.external.repository;

import com.company.finance_api.portfolio.external.domain.ExternalPositionLot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExternalPositionLotRepository extends JpaRepository<ExternalPositionLot, Long> {

    List<ExternalPositionLot> findAllByPortfolioIdAndDeletedFalseOrderByAcquiredAtAsc(Long portfolioId);

    Optional<ExternalPositionLot> findByIdAndPortfolioIdAndDeletedFalse(Long id, Long portfolioId);
}

