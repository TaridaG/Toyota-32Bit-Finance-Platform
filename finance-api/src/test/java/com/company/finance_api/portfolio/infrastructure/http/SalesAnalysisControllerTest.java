package com.company.finance_api.portfolio.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.portfolio.application.SalesAnalysisService;
import com.company.finance_api.portfolio.infrastructure.http.dto.SalesAnalysisPageResponse;
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

@WebMvcTest(controllers = SalesAnalysisController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class SalesAnalysisControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private SalesAnalysisService salesAnalysisService;

  @Test
  void mySalesAnalysisPage_returnsPagedResponse() throws Exception {
    when(salesAnalysisService.getMySalesAnalysisPage(0, 20, null, null, null, null))
        .thenReturn(new SalesAnalysisPageResponse(List.of(), 0, 20, 0, 0));

    mockMvc
        .perform(get("/api/v1/history/sales-analysis/page"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0))
        .andExpect(jsonPath("$.data.size").value(20))
        .andExpect(jsonPath("$.data.totalElements").value(0));
  }
}
