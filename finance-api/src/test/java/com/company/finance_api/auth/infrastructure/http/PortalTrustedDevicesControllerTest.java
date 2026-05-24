package com.company.finance_api.auth.infrastructure.http;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.auth.PortalTrustedDeviceService;
import com.company.finance_api.dto.PortalTrustedDevicesResponseDto;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PortalTrustedDevicesController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class PortalTrustedDevicesControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PortalTrustedDeviceService portalTrustedDeviceService;
  @MockBean private CurrentUserResolver currentUserResolver;

  @Test
  void list_returnsDevices() throws Exception {
    UUID userId = UUID.randomUUID();
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(portalTrustedDeviceService.listForUser(eq(userId), org.mockito.ArgumentMatchers.any()))
        .thenReturn(new PortalTrustedDevicesResponseDto(true, List.of()));

    mockMvc
        .perform(get("/api/portal/profile/trusted-devices"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.devices").isArray());
  }

  @Test
  void revokeOne_delegatesToService() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID deviceId = UUID.randomUUID();
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);

    mockMvc
        .perform(delete("/api/portal/profile/trusted-devices/" + deviceId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    verify(portalTrustedDeviceService)
        .revokeDeviceForUser(eq(userId), eq(deviceId), org.mockito.ArgumentMatchers.any());
  }
}
