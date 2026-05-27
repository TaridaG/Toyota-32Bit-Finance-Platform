package com.company.finance_api.alarm.infrastructure.http;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.alarm.domain.AlarmRule;
import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.AlarmCondition;
import com.company.finance_api.domain.enums.Exchange;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.alarm.application.AlarmService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AlarmController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class AlarmControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AlarmService alarmService;
  @MockBean private CurrentUserResolver currentUserResolver;

  @Test
  void getUserAlarms_returnsMappedResponse() throws Exception {
    UUID userId = UUID.randomUUID();
    User user = new User("user@example.com", "trader");
    Instrument instrument =
        new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE);
    AlarmRule rule =
        new AlarmRule(user, instrument, AlarmCondition.GREATER_THAN, BigDecimal.valueOf(50000));

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(alarmService.getActiveAlarmsForUser(userId)).thenReturn(List.of(rule));

    mockMvc
        .perform(get("/api/v1/alarms"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].instrumentSymbol").value("BTCUSDT"))
        .andExpect(jsonPath("$.data[0].condition").value("GREATER_THAN"))
        .andExpect(jsonPath("$.data[0].active").value(true));
  }

  @Test
  void createAlarm_delegatesToService() throws Exception {
    UUID userId = UUID.randomUUID();
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);

    mockMvc
        .perform(
            post("/api/v1/alarms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "instrumentId": 3,
                      "condition": "LESS_THAN",
                      "threshold": 100.5
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(alarmService)
        .createAlarm(userId, 3L, AlarmCondition.LESS_THAN, new BigDecimal("100.5"));
  }

  @Test
  void deactivateAlarm_delegatesToService() throws Exception {
    UUID userId = UUID.randomUUID();
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);

    mockMvc
        .perform(delete("/api/v1/alarms/9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(alarmService).deactivateAlarm(eq(9L), eq(userId));
  }
}
