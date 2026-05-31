package com.company.finance_api.admin.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.finance_api.profile.domain.User;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPortfolioRepository;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(AdminPortalPortfolioMetricsService.class)
@ActiveProfiles("test")
class AdminPortalPortfolioMetricsServiceTest {

  @Autowired private UserRepository userRepository;

  @Autowired private ExternalPortfolioRepository externalPortfolioRepository;

  @Autowired private AdminPortalPortfolioMetricsService service;

  @Test
  void snapshot_countsExternalPortfolios() {
    User u = new User("pf@x.com", "user_pf");
    userRepository.save(u);
    externalPortfolioRepository.save(new ExternalPortfolio(u, "Ana", "TRY"));

    var dto = service.snapshot();
    assertThat(dto.totalPortfolios()).isEqualTo(1);
    assertThat(dto.newPortfoliosDailyLast7Utc()).hasSize(7);
    assertThat(dto.portfolioUpdatesExistingDailyLast7Utc()).hasSize(7);
  }
}
