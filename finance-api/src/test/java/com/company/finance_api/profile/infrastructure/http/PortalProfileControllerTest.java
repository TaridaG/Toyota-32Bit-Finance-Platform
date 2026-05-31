package com.company.finance_api.profile.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.dto.PortalProfileResponse;
import com.company.finance_api.profile.application.PortalProfileService;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PortalProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class PortalProfileControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PortalProfileService portalProfileService;

  @Test
  void getProfile_returnsProfilePayload() throws Exception {
    when(portalProfileService.getProfile())
        .thenReturn(
            new PortalProfileResponse(
                "trader@example.com",
                "trader",
                null,
                true,
                false,
                null,
                "en",
                "USD",
                false));

    mockMvc
        .perform(get("/api/v1/portal/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.username").value("trader"));
  }
}
