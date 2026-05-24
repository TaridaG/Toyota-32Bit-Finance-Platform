package com.company.newsservice.admin.infrastructure.http;

import com.company.newsservice.admin.application.BuildAdminNewsAnalyticsUseCase;
import com.company.newsservice.admin.application.GetAdminNewsDashboardUseCase;
import com.company.newsservice.admin.infrastructure.http.dto.AdminNewsAnalyticsDashboardDto;
import com.company.newsservice.admin.infrastructure.http.dto.AdminNewsAnalyticsSummaryDto;
import com.company.newsservice.admin.infrastructure.http.dto.AdminNewsDashboardMetricsDto;
import com.company.newsservice.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminNewsMetricsControllerTest {

    @Mock
    private GetAdminNewsDashboardUseCase adminNewsMetricsService;
    @Mock
    private BuildAdminNewsAnalyticsUseCase adminNewsAnalyticsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminNewsMetricsController controller = new AdminNewsMetricsController(
                adminNewsMetricsService,
                adminNewsAnalyticsService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void dashboard_returnsSuccessPayload() throws Exception {
        when(adminNewsMetricsService.snapshot()).thenReturn(new AdminNewsDashboardMetricsDto(
                120L,
                8,
                List.of(1, 2, 3, 4, 5, 6, 7),
                Instant.parse("2026-05-23T10:00:00Z")
        ));

        mockMvc.perform(get("/api/news/admin/metrics/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalArticles").value(120));
    }

    @Test
    void analytics_returnsBadRequestWhenOnlyFromProvided() throws Exception {
        mockMvc.perform(get("/api/news/admin/metrics/analytics")
                        .param("from", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void analytics_returnsCustomRangePayload() throws Exception {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 7);
        when(adminNewsAnalyticsService.dashboardCustom(from, to)).thenReturn(sampleAnalyticsDashboard());

        mockMvc.perform(get("/api/news/admin/metrics/analytics")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.totalArticles").value(100));
    }

    @Test
    void analytics_propagatesInvalidRangeAsBadRequest() throws Exception {
        when(adminNewsAnalyticsService.dashboardCustom(any(), any()))
                .thenThrow(new IllegalArgumentException("from must be on or before to"));

        mockMvc.perform(get("/api/news/admin/metrics/analytics")
                        .param("from", "2026-05-10")
                        .param("to", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.message").value("from must be on or before to"));
    }

    private static AdminNewsAnalyticsDashboardDto sampleAnalyticsDashboard() {
        return new AdminNewsAnalyticsDashboardDto(
                "custom",
                Instant.parse("2026-05-01T00:00:00Z"),
                Instant.parse("2026-05-08T00:00:00Z"),
                new AdminNewsAnalyticsSummaryDto(100L, 5, 80.0, "en", 1, 2, 3, 4, 5.0),
                List.of(),
                List.of(),
                Instant.parse("2026-05-23T10:00:00Z")
        );
    }
}
