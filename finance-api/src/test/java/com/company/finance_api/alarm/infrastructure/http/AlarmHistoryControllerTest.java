package com.company.finance_api.alarm.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.dto.AlarmHistoryResponse;
import com.company.finance_api.service.AlarmHistoryService;
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

@WebMvcTest(controllers = AlarmHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class AlarmHistoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AlarmHistoryService alarmHistoryService;

  @Test
  void getHistory_returnsTimeline() throws Exception {
    when(alarmHistoryService.getMyTimeline())
        .thenReturn(
            List.of(
                new AlarmHistoryResponse(
                    "BTCUSDT", "GREATER_THAN", BigDecimal.valueOf(50000), Instant.now())));

    mockMvc
        .perform(get("/api/history/alarms"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].instrumentSymbol").value("BTCUSDT"));
  }
}
