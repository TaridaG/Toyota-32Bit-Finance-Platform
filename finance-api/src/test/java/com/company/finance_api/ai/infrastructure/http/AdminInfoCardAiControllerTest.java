package com.company.finance_api.ai.infrastructure.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.ai.dto.InfoCardAiContentResponse;
import com.company.finance_api.ai.service.AdminInfoCardAiService;
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

@WebMvcTest(controllers = AdminInfoCardAiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class AdminInfoCardAiControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AdminInfoCardAiService adminInfoCardAiService;

  @Test
  void complete_returnsPayload() throws Exception {
    when(adminInfoCardAiService.complete(any()))
        .thenReturn(
            new InfoCardAiContentResponse(
                null, null, "Detay", "Yorum", "Hata", "Ornek", List.of("A")));

    mockMvc
        .perform(
            post("/api/admin/info-cards/ai/complete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "language": "tr",
                                  "title": "Tahvil",
                                  "shortDescription": "Kisa"
                                }
                                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.detailedDescription").value("Detay"));
  }
}
