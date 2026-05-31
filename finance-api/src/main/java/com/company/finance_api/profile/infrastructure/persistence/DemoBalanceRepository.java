package com.company.finance_api.profile.infrastructure.persistence;

import com.company.finance_api.profile.domain.DemoBalance;
import com.company.finance_api.profile.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** DemoBalance entity persistence için Spring Data repository. */
public interface DemoBalanceRepository extends JpaRepository<DemoBalance, UUID> {

  Optional<DemoBalance> findByUser(User user);
}
