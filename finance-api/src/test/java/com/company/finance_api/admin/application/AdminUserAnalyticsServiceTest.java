package com.company.finance_api.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.company.finance_api.admin.domain.AdminUserAnalyticsPreset;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminUserAnalyticsServiceTest {

  @Mock UserRepository userRepository;
  @Mock AdminPortalRosterCounterService adminPortalRosterCounterService;

  @InjectMocks AdminUserAnalyticsService service;

  @Test
  void maskEmail_obfuscatesLocalPart() {
    assertThat(AdminUserAnalyticsService.maskEmail("john.doe@Example.COM"))
        .isEqualTo("j***e@example.com");
    assertThat(AdminUserAnalyticsService.maskEmail(null)).isEqualTo("—");
    assertThat(AdminUserAnalyticsService.maskEmail("bad")).isEqualTo("—");
  }

  @Test
  void dashboardCustom_rejectsNullBounds() {
    assertThatThrownBy(() -> service.dashboardCustom(null, LocalDate.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("from and to are required");
  }

  @Test
  void dashboardCustom_rejectsReverseRange() {
    LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
    assertThatThrownBy(() -> service.dashboardCustom(today, today.minusDays(1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("from must be on or before to");
  }

  @Test
  void dashboard_buildsSummaryFromRepositoryCounts() {
    when(userRepository.countByNotPendingDeletion()).thenReturn(10L);
    when(userRepository.countActiveRoster()).thenReturn(8L);
    when(userRepository.countCreatedInRangeExcludingPendingDeletion(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(2L);
    when(userRepository.countDeletionRequestedInRange(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(0L);
    when(userRepository.countFrozenRoster()).thenReturn(1L);
    when(userRepository.findRosterByCreatedAtDesc(org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());
    when(adminPortalRosterCounterService.deletedAccountsTotal()).thenReturn(3L);

    var dashboard = service.dashboard(AdminUserAnalyticsPreset.LAST_7_DAYS);

    assertThat(dashboard.preset()).isEqualTo("7d");
    assertThat(dashboard.summary().totalUsers()).isEqualTo(10L);
    assertThat(dashboard.summary().activeUsers()).isEqualTo(8L);
    assertThat(dashboard.dailyRegistrations()).hasSize(7);
  }
}
