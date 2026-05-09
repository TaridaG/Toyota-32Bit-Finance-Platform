package com.company.finance_api.repository;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.User;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByUserOrderByCreatedAtDesc(User user);
    List<Transaction> findByUserAndExternalPortfolioOrderByCreatedAtDesc(User user, ExternalPortfolio externalPortfolio);
    List<Transaction> findByUserAndExternalPortfolioOrderByCreatedAtAsc(User user, ExternalPortfolio externalPortfolio);
    List<Transaction> findByUserAndInstrument(User user, Instrument instrument);
    List<Transaction> findByUserAndInstrumentAndExternalPortfolio(User user, Instrument instrument, ExternalPortfolio externalPortfolio);
}