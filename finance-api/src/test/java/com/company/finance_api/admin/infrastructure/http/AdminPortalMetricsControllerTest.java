package com.company.finance_api.admin.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.admin.application.AdminBlockedEmailDirectoryService;
import com.company.finance_api.admin.application.AdminLatencyProbeService;
import com.company.finance_api.admin.application.AdminMarketAssetAnalyticsService;
import com.company.finance_api.admin.application.AdminPortalInstrumentActivityService;
import com.company.finance_api.admin.application.AdminPortalPortfolioMetricsService;
import com.company.finance_api.admin.application.AdminPortalUserMetricsService;
import com.company.finance_api.admin.application.AdminPortfolioAnalyticsService;
import com.company.finance_api.admin.application.AdminUserAccountService;
import com.company.finance_api.admin.application.AdminUserAnalyticsService;
import com.company.finance_api.admin.application.AdminUserDirectoryService;
import com.company.finance_api.admin.application.AdminUserPortfolioDetailsService;
import com.company.finance_api.admin.infrastructure.http.dto.AdminPortalPortfolioMetricsDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminPortalUserMetricsDto;
import com.company.finance_api.profile.application.PortalProfileService;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminPortalMetricsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class AdminPortalMetricsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AdminPortalUserMetricsService adminPortalUserMetricsService;
  @MockBean private AdminPortalPortfolioMetricsService adminPortalPortfolioMetricsService;
  @MockBean private AdminPortalInstrumentActivityService adminPortalInstrumentActivityService;
  @MockBean private InstrumentRepository instrumentRepository;
  @MockBean private AdminLatencyProbeService adminLatencyProbeService;
  @MockBean private AdminUserDirectoryService adminUserDirectoryService;
  @MockBean private AdminUserPortfolioDetailsService adminUserPortfolioDetailsService;
  @MockBean private AdminUserAnalyticsService adminUserAnalyticsService;
  @MockBean private AdminPortfolioAnalyticsService adminPortfolioAnalyticsService;
  @MockBean private PortalProfileService portalProfileService;
  @MockBean private AdminUserAccountService adminUserAccountService;
  @MockBean private AdminMarketAssetAnalyticsService adminMarketAssetAnalyticsService;
  @MockBean private AdminBlockedEmailDirectoryService adminBlockedEmailDirectoryService;

  @Test
  void portalUsers_returnsDashboardMetrics() throws Exception {
    Instant generatedAt = Instant.parse("2026-01-01T00:00:00Z");
    when(adminPortalUserMetricsService.snapshot())
        .thenReturn(
            new AdminPortalUserMetricsDto(10, 2, 1, 100.0, List.of(1), List.of(0), generatedAt));
    when(adminPortalPortfolioMetricsService.snapshot())
        .thenReturn(AdminPortalPortfolioMetricsDto.empty(generatedAt));
    when(instrumentRepository.countByActiveTrue()).thenReturn(5L);
    when(adminPortalInstrumentActivityService.distinctInstrumentsWithPriceDailyLast7Utc(
            generatedAt))
        .thenReturn(List.of(1, 2));

    mockMvc
        .perform(get("/api/v1/admin/metrics/portal-users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.totalUsers").value(10))
        .andExpect(jsonPath("$.data.totalInstruments").value(5));
  }
}
