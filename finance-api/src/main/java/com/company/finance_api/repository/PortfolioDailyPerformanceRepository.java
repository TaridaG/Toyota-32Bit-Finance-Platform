package com.company.finance_api.repository;

import com.company.finance_api.domain.PortfolioDailyPerformance;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository for precomputed daily portfolio performance rows. */
public interface PortfolioDailyPerformanceRepository
    extends JpaRepository<PortfolioDailyPerformance, Long> {

  List<PortfolioDailyPerformance> findByUserIdAndExternalPortfolioIdAndCurrencyOrderByDayUtcAsc(
      UUID userId, Long externalPortfolioId, String currency);

  List<PortfolioDailyPerformance>
      findByUserIdAndExternalPortfolioIdAndCurrencyAndDayUtcGreaterThanEqualOrderByDayUtcAsc(
          UUID userId, Long externalPortfolioId, String currency, LocalDate dayUtc);

  List<PortfolioDailyPerformance> findByUserIdAndCurrencyOrderByDayUtcAsc(
      UUID userId, String currency);

  List<PortfolioDailyPerformance> findByUserIdAndCurrencyAndDayUtcGreaterThanEqualOrderByDayUtcAsc(
      UUID userId, String currency, LocalDate dayUtc);

  Optional<PortfolioDailyPerformance>
      findTopByUserIdAndExternalPortfolioIdAndCurrencyOrderByDayUtcDesc(
          UUID userId, Long externalPortfolioId, String currency);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      delete from PortfolioDailyPerformance p
      where p.userId = :userId
        and p.externalPortfolioId = :portfolioId
        and p.currency = :currency
      """)
  void deleteByUserIdAndExternalPortfolioIdAndCurrency(
      @Param("userId") UUID userId,
      @Param("portfolioId") Long portfolioId,
      @Param("currency") String currency);

  @Query(
      """
      select count(p) > 0 from PortfolioDailyPerformance p
      where p.userId = :userId
        and p.externalPortfolioId = :portfolioId
        and p.currency = :currency
        and p.dayUtc = :dayUtc
      """)
  boolean existsForDay(
      @Param("userId") UUID userId,
      @Param("portfolioId") Long portfolioId,
      @Param("currency") String currency,
      @Param("dayUtc") LocalDate dayUtc);
}
