package com.company.finance_api.portfolio.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.portfolio.application.TransactionHistoryService;
import com.company.finance_api.portfolio.infrastructure.http.dto.TransactionHistoryResponse;
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

@WebMvcTest(controllers = TransactionHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class TransactionHistoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private TransactionHistoryService transactionHistoryService;

  @Test
  void getHistory_returnsMappedRows() throws Exception {
    when(transactionHistoryService.getMyHistory(null))
        .thenReturn(
            List.of(
                new TransactionHistoryResponse(
                    1L,
                    null,
                    null,
                    "AAPL",
                    "BUY",
                    "NOW",
                    "NOW_BOUGHT",
                    BigDecimal.ONE,
                    BigDecimal.TEN,
                    BigDecimal.TEN,
                    "USD",
                    BigDecimal.TEN,
                    BigDecimal.ONE,
                    Instant.now(),
                    Instant.now(),
                    "USD")));

    mockMvc
        .perform(get("/api/v1/history/transactions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].instrumentSymbol").value("AAPL"));
  }
}
