package com.company.finance_api.auth.infrastructure.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.auth.domain.LoginCompletionResult;
import com.company.finance_api.auth.application.PortalLoginService;
import com.company.finance_api.auth.application.PublicPasswordResetService;
import com.company.finance_api.auth.infrastructure.http.dto.PublicLoginResponse;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PublicAuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class PublicAuthenticationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PortalLoginService portalLoginService;

  @MockBean private PublicPasswordResetService publicPasswordResetService;

  @Test
  void login_returnsSuccessEnvelope() throws Exception {
    PublicLoginResponse tokens =
        PublicLoginResponse.complete("access-token", 300L, "Bearer", "refresh-token", 1800L);
    when(portalLoginService.login(any(), any(), any()))
        .thenReturn(LoginCompletionResult.of(tokens));

    mockMvc
        .perform(
            post("/api/v1/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"trader@example.com","password":"secret"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.status").value("COMPLETE"))
        .andExpect(jsonPath("$.data.accessToken").value("access-token"));
  }

  @Test
  void login_validationError_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"username":"","password":""}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }
}
