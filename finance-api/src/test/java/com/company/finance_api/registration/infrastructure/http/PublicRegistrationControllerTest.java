package com.company.finance_api.registration.infrastructure.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.dto.PublicSendVerificationCodeResponse;
import com.company.finance_api.dto.PublicUsernameAvailabilityResponse;
import com.company.finance_api.registration.PortalRegistrationService;
import com.company.finance_api.registration.RegistrationEmailVerificationService;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PublicRegistrationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class PublicRegistrationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PortalRegistrationService portalRegistrationService;
  @MockBean private RegistrationEmailVerificationService registrationEmailVerificationService;

  @Test
  void sendCode_returnsSuccessEnvelope() throws Exception {
    when(registrationEmailVerificationService.sendCode(eq("new@example.com"), any()))
        .thenReturn(new PublicSendVerificationCodeResponse(300, 60));

    mockMvc
        .perform(
            post("/api/v1/public/register/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email":"new@example.com","locale":"en"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.expiresInSeconds").value(300));
  }

  @Test
  void checkUsernameAvailability_returnsAvailabilityPayload() throws Exception {
    when(portalRegistrationService.checkUsernameAvailability("trader"))
        .thenReturn(new PublicUsernameAvailabilityResponse("trader", true, List.of()));

    mockMvc
        .perform(get("/api/v1/public/register/username-availability").param("username", "trader"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.available").value(true));
  }

  @Test
  void register_validationError_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/public/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }
}
