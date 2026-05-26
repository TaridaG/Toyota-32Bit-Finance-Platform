package com.company.finance_api.portfolio.infrastructure.http;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.enums.PurchaseMode;
import com.company.finance_api.domain.enums.TradeInputMode;
import com.company.finance_api.domain.enums.TransactionType;
import com.company.finance_api.dto.InstrumentPriceCoverageResponse;
import com.company.finance_api.dto.TradePreviewResponse;
import com.company.finance_api.portfolio.application.TradeService;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import java.math.BigDecimal;
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

@WebMvcTest(controllers = TradeController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class TradeControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private TradeService tradeService;

  @Test
  void preview_returnsSuccessEnvelope() throws Exception {
    TradePreviewResponse preview =
        new TradePreviewResponse(
            1L,
            "AAPL",
            "USD",
            new BigDecimal("2"),
            new BigDecimal("300"),
            "USD",
            new BigDecimal("150"),
            BigDecimal.ONE,
            false,
            "LIVE_MARKET_DATA",
            null,
            false,
            null);
    when(tradeService.preview(any())).thenReturn(preview);

    mockMvc
        .perform(
            post("/api/trades/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "instrumentId": 1,
                      "inputMode": "LOTS",
                      "lots": 2,
                      "inputCurrency": "USD",
                      "purchaseMode": "NOW"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.instrumentSymbol").value("AAPL"));
  }

  @Test
  void priceCoverage_returnsCoveragePayload() throws Exception {
    when(tradeService.getPriceCoverage(7L))
        .thenReturn(
            new InstrumentPriceCoverageResponse(
                7L, "AAPL", Instant.parse("2020-01-01T00:00:00Z"), Instant.now()));

    mockMvc
        .perform(get("/api/trades/instruments/7/price-coverage"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.instrumentId").value(7));
  }

  @Test
  void buyOrder_validationError_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/trades/buy/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
  }

  @Test
  void buy_returnsTransactionPayload() throws Exception {
    Transaction tx =
        Transaction.buy(null, null, BigDecimal.TEN, BigDecimal.ONE);
    when(tradeService.buy(3L, new BigDecimal("1.5"))).thenReturn(tx);

    mockMvc
        .perform(
            post("/api/trades/buy")
                .param("instrumentId", "3")
                .param("quantity", "1.5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }
}
