package com.company.finance_api.portfolio.external.infrastructure.http;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.portfolio.external.infrastructure.http.dto.CreateExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.infrastructure.http.dto.ExternalPortfolioResponse;
import com.company.finance_api.portfolio.external.application.ExternalPortfolioService;
import com.company.finance_api.portfolio.external.application.ExternalPortfolioValuationService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ExternalPortfolioController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class ExternalPortfolioControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private ExternalPortfolioService service;
  @MockBean private ExternalPortfolioValuationService valuationService;
  @MockBean private CurrentUserResolver currentUserResolver;

  @Test
  void list_returnsPortfolios() throws Exception {
    UUID userId = UUID.randomUUID();
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(service.getUserPortfolios(userId)).thenReturn(List.of());

    mockMvc
        .perform(get("/api/v1/external/portfolios"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").isArray());
  }

  @Test
  void create_delegatesToService() throws Exception {
    UUID userId = UUID.randomUUID();
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(service.createPortfolio(eq(userId), org.mockito.ArgumentMatchers.any(CreateExternalPortfolioRequest.class)))
        .thenReturn(
            ExternalPortfolioResponse.builder()
                .id(1L)
                .name("Growth")
                .baseCurrency("USD")
                .amountsHidden(false)
                .build());

    mockMvc
        .perform(
            post("/api/v1/external/portfolios")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"Growth","baseCurrency":"USD"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.name").value("Growth"));

    verify(service).createPortfolio(eq(userId), org.mockito.ArgumentMatchers.any());
  }
}
