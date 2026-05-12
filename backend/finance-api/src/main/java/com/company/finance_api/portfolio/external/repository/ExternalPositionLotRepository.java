package com.company.finance_api.portfolio.external.repository;

import com.company.finance_api.portfolio.external.domain.ExternalPositionLot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExternalPositionLotRepository extends JpaRepository<ExternalPositionLot, Long> {

    long countByDeletedFalse();

    List<ExternalPositionLot> findAllByPortfolioIdAndDeletedFalseOrderByAcquiredAtAsc(Long portfolioId);

    Optional<ExternalPositionLot> findByIdAndPortfolioIdAndDeletedFalse(Long id, Long portfolioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM external_position_lots WHERE portfolio_id = :portfolioId", nativeQuery = true)
    void deleteAllByPortfolioId(@Param("portfolioId") Long portfolioId);
}

