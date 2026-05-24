package com.company.finance_api.admin.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.finance_api.domain.User;
import com.company.finance_api.repository.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(AdminPortalUserMetricsService.class)
@ActiveProfiles("test")
class AdminPortalUserMetricsServiceTest {

  @Autowired private UserRepository userRepository;

  @Autowired private AdminPortalUserMetricsService service;

  @Test
  void weekOverWeekPercent_handlesZeroPrevious() {
    assertThat(AdminPortalUserMetricsService.weekOverWeekPercent(3, 0)).isEqualTo(100.0);
    assertThat(AdminPortalUserMetricsService.weekOverWeekPercent(0, 0)).isEqualTo(0.0);
    assertThat(AdminPortalUserMetricsService.weekOverWeekPercent(1, 4)).isEqualTo(-75.0);
  }

  @Test
  void snapshot_countsActiveRosterAndSkipsDeletionPending() {
    User a = new User("a@x.com", "user_a");
    User b = new User("b@x.com", "user_b");
    userRepository.save(a);
    userRepository.save(b);
    b.markDeletionRequested(Instant.now());
    userRepository.save(b);

    var dto = service.snapshot();
    assertThat(dto.totalUsers()).isEqualTo(1);
    assertThat(dto.newRegistrationsDailyLast7Utc()).hasSize(7);
    assertThat(dto.userDeletionRequestsDailyLast7Utc()).hasSize(7);
  }
}
