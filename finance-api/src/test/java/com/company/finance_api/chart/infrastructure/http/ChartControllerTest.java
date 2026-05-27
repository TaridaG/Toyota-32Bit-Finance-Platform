package com.company.finance_api.chart.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.chart.infrastructure.http.dto.CandlestickResponse;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.chart.application.ChartService;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import java.math.BigDecimal;
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

@WebMvcTest(controllers = ChartController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class ChartControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ChartService chartService;

  @Test
  void candles_returnsSeries() throws Exception {
    Instant ts = Instant.parse("2026-01-01T00:00:00Z");
    when(chartService.getCandlesticks(
            1L, Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-02T00:00:00Z"), PriceType.MARKET))
        .thenReturn(
            List.of(new CandlestickResponse(ts, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN)));

    mockMvc
        .perform(
            get("/api/v1/charts/1/candles")
                .param("from", "2026-01-01T00:00:00Z")
                .param("to", "2026-01-02T00:00:00Z")
                .param("priceType", "MARKET"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].close").value(10));
  }
}
