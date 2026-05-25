package com.company.finance_api.portfolio.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.dto.PortfolioOverviewResponse;
import com.company.finance_api.dto.PortfolioPerformanceSeriesResponse;
import com.company.finance_api.dto.PortfolioSummaryResponse;
import com.company.finance_api.service.PortfolioOverviewService;
import com.company.finance_api.service.PortfolioPerformanceSeriesService;
import com.company.finance_api.service.PortfolioService;
import com.company.finance_api.service.PortfolioSnapshotService;
import com.company.finance_api.service.PortfolioTradeFlowService;
import com.company.finance_api.service.PortfolioValuationService;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PortfolioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class PortfolioControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PortfolioService portfolioService;
  @MockBean private PortfolioOverviewService portfolioOverviewService;
  @MockBean private PortfolioPerformanceSeriesService portfolioPerformanceSeriesService;
  @MockBean private PortfolioSnapshotService portfolioSnapshotService;
  @MockBean private PortfolioValuationService portfolioValuationService;
  @MockBean private PortfolioTradeFlowService portfolioTradeFlowService;

  @Test
  void myPortfolio_returnsSuccessEnvelope() throws Exception {
    when(portfolioService.getMyPortfolio()).thenReturn(Collections.emptyList());

    mockMvc
        .perform(get("/api/portfolio"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  void summary_returnsSummaryPayload() throws Exception {
    when(portfolioService.getPortfolioSummary())
        .thenReturn(
            new PortfolioSummaryResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

    mockMvc
        .perform(get("/api/portfolio/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void overview_returnsOverviewPayload() throws Exception {
    when(portfolioOverviewService.getMyOverview("USD", null))
        .thenReturn(
            new PortfolioOverviewResponse(
                "USD",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                List.of()));

    mockMvc
        .perform(get("/api/portfolio/overview").header("X-Currency", "USD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.currency").value("USD"));
  }

  @Test
  void performanceSeries_returnsSeriesPayload() throws Exception {
    when(portfolioPerformanceSeriesService.getMyPerformanceSeries("USD", 7L, "1w"))
        .thenReturn(new PortfolioPerformanceSeriesResponse("USD", null, List.of()));

    mockMvc
        .perform(
            get("/api/portfolio/performance-series")
                .param("portfolioId", "7")
                .param("range", "1w")
                .header("X-Currency", "USD"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.currency").value("USD"));
  }
}
