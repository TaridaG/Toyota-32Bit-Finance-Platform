package com.company.finance_api.mfa.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.dto.PortalMfaStatusResponse;
import com.company.finance_api.mfa.PortalMfaService;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PortalMfaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class PortalMfaControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PortalMfaService portalMfaService;

  @Test
  void status_returnsMfaState() throws Exception {
    when(portalMfaService.getStatus())
        .thenReturn(new PortalMfaStatusResponse(true, Instant.parse("2026-01-01T00:00:00Z")));

    mockMvc
        .perform(get("/api/v1/portal/profile/mfa"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.enabled").value(true));
  }

  @Test
  void confirm_validationError_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/portal/profile/mfa/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"abc\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }
}
