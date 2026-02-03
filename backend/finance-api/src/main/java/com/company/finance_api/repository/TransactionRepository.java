package com.company.finance_api.repository;

import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserOrderByCreatedAtDesc(User user);
}