package com.company.finance_api.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.company.finance_api.admin.domain.AdminUserAnalyticsPreset;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.external.repository.ExternalPositionLotRepository;
import com.company.finance_api.repository.UserRepository;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminPortfolioAnalyticsServiceTest {

  @Mock ExternalPortfolioRepository externalPortfolioRepository;
  @Mock ExternalPositionLotRepository externalPositionLotRepository;
  @Mock UserRepository userRepository;

  @InjectMocks AdminPortfolioAnalyticsService service;

  @Test
  void dashboardCustom_rejectsRangeOverMaxDays() {
    LocalDate today = LocalDate.now(java.time.ZoneOffset.UTC);
    LocalDate from = today.minusDays(AdminPortfolioAnalyticsService.MAX_CUSTOM_RANGE_DAYS + 1);

    assertThatThrownBy(() -> service.dashboardCustom(from, today))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("range must not exceed");
  }

  @Test
  void dashboard_buildsSummaryAndDailySeries() {
    when(externalPortfolioRepository.count()).thenReturn(5L);
    when(externalPositionLotRepository.countByDeletedFalse()).thenReturn(10L);
    when(userRepository.countByNotPendingDeletion()).thenReturn(4L);
    when(externalPortfolioRepository.countCreatedBetween(any(Timestamp.class), any(Timestamp.class)))
        .thenReturn(1L);
    when(externalPortfolioRepository.findAllWithUserOrderByCreatedAtDesc(any()))
        .thenReturn(List.of());

    var dashboard = service.dashboard(AdminUserAnalyticsPreset.LAST_7_DAYS);

    assertThat(dashboard.preset()).isEqualTo("7d");
    assertThat(dashboard.summary().totalPortfolios()).isEqualTo(5L);
    assertThat(dashboard.summary().averageOpenLotsPerPortfolio()).isEqualTo(2.0);
    assertThat(dashboard.dailyCreations()).hasSize(7);
  }
}
