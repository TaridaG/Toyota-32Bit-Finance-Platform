package com.company.finance_api.admin;

import com.company.finance_api.domain.User;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(AdminPortalPortfolioMetricsService.class)
@ActiveProfiles("test")
class AdminPortalPortfolioMetricsServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExternalPortfolioRepository externalPortfolioRepository;

    @Autowired
    private AdminPortalPortfolioMetricsService service;

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
