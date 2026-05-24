package com.company.finance_api.market.infrastructure.http;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MarketFundamentalsPassthroughController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, ObjectMapper.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class MarketFundamentalsPassthroughControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void fundamentals_tryFxCross_returnsSyntheticPayloadWithoutMds() throws Exception {
    mockMvc
        .perform(get("/api/market/instruments/USDTRY/fundamentals"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.symbol").value("USDTRY"))
        .andExpect(jsonPath("$.currency").value("TRY"));
  }
}
