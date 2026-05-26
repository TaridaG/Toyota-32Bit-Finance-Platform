package com.company.finance_api.notification.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.notification.application.PortalNotificationService;
import com.company.finance_api.notification.infrastructure.http.dto.PortalNotificationPageResponse;
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

@WebMvcTest(controllers = PortalNotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class PortalNotificationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private PortalNotificationService notificationService;

  @Test
  void unreadCount_returnsCount() throws Exception {
    when(notificationService.getUnreadCount()).thenReturn(3L);

    mockMvc
        .perform(get("/api/notifications/unread-count"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(3));
  }

  @Test
  void myNotifications_returnsPage() throws Exception {
    when(notificationService.getMyPage(0, 20))
        .thenReturn(new PortalNotificationPageResponse(List.of(), 0, 20, 0, 0, 0));

    mockMvc
        .perform(get("/api/notifications"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.page").value(0));
  }
}
