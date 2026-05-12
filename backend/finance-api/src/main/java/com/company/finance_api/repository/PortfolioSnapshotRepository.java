package com.company.finance_api.repository;

import com.company.finance_api.domain.PortfolioSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PortfolioSnapshotRepository extends JpaRepository<PortfolioSnapshot, Long> {

    List<PortfolioSnapshot> findByUserIdOrderByCreatedAtAsc(UUID userId);

    List<PortfolioSnapshot> findByUserIdAndExternalPortfolioIdOrderByCreatedAtAsc(
            UUID userId, Long externalPortfolioId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM portfolio_snapshots WHERE external_portfolio_id = :portfolioId", nativeQuery = true)
    void deleteAllByExternalPortfolioId(@Param("portfolioId") Long portfolioId);
}