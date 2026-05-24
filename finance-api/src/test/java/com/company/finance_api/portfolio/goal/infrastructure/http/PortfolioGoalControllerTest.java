package com.company.finance_api.portfolio.goal.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.portfolio.goal.PortfolioGoalService;
import com.company.finance_api.portfolio.goal.dto.PortfolioGoalsViewResponse;
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

@WebMvcTest(controllers = PortfolioGoalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class PortfolioGoalControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PortfolioGoalService portfolioGoalService;

  @Test
  void getGoals_returnsView() throws Exception {
    when(portfolioGoalService.getGoals(null, null))
        .thenReturn(new PortfolioGoalsViewResponse("ALL", null, "USD", null, null));

    mockMvc
        .perform(get("/api/portfolio/goals"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.scope").value("ALL"));
  }
}
