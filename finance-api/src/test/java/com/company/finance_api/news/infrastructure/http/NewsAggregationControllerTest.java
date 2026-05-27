package com.company.finance_api.news.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.news.infrastructure.http.dto.NewsEnrichedPageResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsWeeklySummaryResponse;
import com.company.finance_api.news.application.NewsEnrichmentService;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = NewsAggregationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class NewsAggregationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private NewsEnrichmentService newsEnrichmentService;

  @Test
  void enrichedPage_returnsPagedNews() throws Exception {
    when(newsEnrichmentService.getEnrichedNews(0, 20, null, null, null, null, null, null, null, null, null))
        .thenReturn(new NewsEnrichedPageResponse(List.of(), 0, 20, 0, 0));

    mockMvc
        .perform(get("/api/v1/news/enriched"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0));
  }

  @Test
  void enrichedPage_forwardsRelatedSymbolsFilter() throws Exception {
    when(newsEnrichmentService.getEnrichedNews(0, 20, null, null, null, null, null, "BTC,ETH", null, null, null))
        .thenReturn(new NewsEnrichedPageResponse(List.of(), 0, 20, 0, 0));

    mockMvc
        .perform(get("/api/v1/news/enriched").param("relatedSymbols", "BTC,ETH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(newsEnrichmentService)
        .getEnrichedNews(0, 20, null, null, null, null, null, "BTC,ETH", null, null, null);
  }

  @Test
  void enrichedPage_forwardsWeeklySummaryFilters() throws Exception {
    when(newsEnrichmentService.getEnrichedNews(
            0, 20, null, null, null, null, null, null, "CoinTelegraph", "BTC", "crypto"))
        .thenReturn(new NewsEnrichedPageResponse(List.of(), 0, 20, 0, 0));

    mockMvc
        .perform(
            get("/api/v1/news/enriched")
                .param("sourceName", "CoinTelegraph")
                .param("assetKey", "BTC")
                .param("primaryTopic", "crypto"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(newsEnrichmentService)
        .getEnrichedNews(0, 20, null, null, null, null, null, null, "CoinTelegraph", "BTC", "crypto");
  }

  @Test
  void weeklySummary_returnsSummaryPayload() throws Exception {
    when(newsEnrichmentService.getWeeklySummary(null, "BTC,ETH"))
        .thenReturn(new NewsWeeklySummaryResponse(227, List.of(), List.of(), List.of(), 12));

    mockMvc
        .perform(get("/api/v1/news/enriched/weekly-summary").param("portfolioSymbols", "BTC,ETH"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.totalCount").value(227))
        .andExpect(jsonPath("$.data.portfolioRelatedCount").value(12));

    verify(newsEnrichmentService).getWeeklySummary(null, "BTC,ETH");
  }
}
