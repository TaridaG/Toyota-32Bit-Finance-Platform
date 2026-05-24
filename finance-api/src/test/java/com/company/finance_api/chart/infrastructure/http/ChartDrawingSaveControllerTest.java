package com.company.finance_api.chart.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.dto.ChartDrawingSavePageResponse;
import com.company.finance_api.service.ChartDrawingSaveService;
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

@WebMvcTest(controllers = ChartDrawingSaveController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class ChartDrawingSaveControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ChartDrawingSaveService chartDrawingSaveService;

  @Test
  void listMine_returnsPage() throws Exception {
    when(chartDrawingSaveService.listPage(0, 10))
        .thenReturn(new ChartDrawingSavePageResponse(List.of(), 0, 10, 0, 0));

    mockMvc
        .perform(get("/api/chart-drawings/mine"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0));
  }
}
