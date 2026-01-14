package com.company.finance_api.repository;

import com.company.finance_api.domain.DemoBalance;
import com.company.finance_api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DemoBalanceRepository extends JpaRepository<DemoBalance, UUID> {

    Optional<DemoBalance> findByUser(User user);
}
