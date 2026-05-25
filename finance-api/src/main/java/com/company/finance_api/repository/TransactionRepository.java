package com.company.finance_api.repository;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.User;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Transaction entity persistence için Spring Data repository. */
public interface TransactionRepository
    extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

  List<Transaction> findByUserOrderByCreatedAtDesc(User user);

  List<Transaction> findByUserOrderByCreatedAtAsc(User user);

  List<Transaction> findByUserAndExternalPortfolioOrderByCreatedAtDesc(
      User user, ExternalPortfolio externalPortfolio);

  List<Transaction> findByUserAndExternalPortfolioOrderByCreatedAtAsc(
      User user, ExternalPortfolio externalPortfolio);

  List<Transaction> findByUserIdAndExternalPortfolioIdOrderByCreatedAtAsc(
      UUID userId, Long externalPortfolioId);

  List<Transaction> findByUserAndInstrument(User user, Instrument instrument);

  List<Transaction> findByUserAndInstrumentAndExternalPortfolio(
      User user, Instrument instrument, ExternalPortfolio externalPortfolio);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value = "DELETE FROM transactions WHERE external_portfolio_id = :portfolioId",
      nativeQuery = true)
  void deleteAllByExternalPortfolioId(@Param("portfolioId") Long portfolioId);

  @Query(
      """
            select t from Transaction t
            join fetch t.instrument
            join fetch t.user
            join fetch t.externalPortfolio
            where t.externalPortfolio is not null
            order by t.createdAt asc, t.id asc
            """)
  List<Transaction> findAllWithExternalPortfolio();
}
