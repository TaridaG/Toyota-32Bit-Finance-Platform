package com.company.finance_api.portfolio.external.repository;

import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalPortfolioRepository extends JpaRepository<ExternalPortfolio, Long> {

    List<ExternalPortfolio> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<ExternalPortfolio> findByIdAndUserId(Long id, UUID userId);

    long countByUserId(UUID userId);

    /** Native SQL avoids HQL edge cases; binds JDBC {@link Timestamp} to {@code created_at}. */
    @Query(
            value = "SELECT COUNT(*) FROM external_portfolios WHERE created_at >= :startInclusive AND created_at < :endExclusive",
            nativeQuery = true)
    long countCreatedBetween(@Param("startInclusive") Timestamp startInclusive, @Param("endExclusive") Timestamp endExclusive);

    /**
     * Portfolios that already existed before the UTC day started and were updated during that day
     * (edits/renames — excludes first-time {@code created_at} on that day).
     */
    @Query(
            value = """
                    SELECT COUNT(*) FROM external_portfolios
                    WHERE created_at < :dayStartUtc
                      AND updated_at >= :dayStartUtc AND updated_at < :dayEndExclusive
                    """,
            nativeQuery = true)
    long countExistingUpdatedOnUtcDay(
            @Param("dayStartUtc") Timestamp dayStartUtc,
            @Param("dayEndExclusive") Timestamp dayEndExclusive);

    @Query("select p from ExternalPortfolio p join fetch p.user order by p.createdAt desc")
    List<ExternalPortfolio> findAllWithUserOrderByCreatedAtDesc(Pageable pageable);
}

