package com.company.finance_api.instrument.infrastructure.http;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.instrument.application.InstrumentService;
import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.shared.web.GlobalExceptionHandler;
import com.company.finance_api.test.support.WebMvcTestSecuritySupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminInstrumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class AdminInstrumentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private InstrumentService instrumentService;

  @Test
  void createInstrument_returnsInstrumentId() throws Exception {
    Instrument instrument =
        new Instrument("AAPL", "Apple", InstrumentType.STOCK, Exchange.NASDAQ);
    ReflectionTestUtils.setField(instrument, "id", 42L);
    when(instrumentService.createInstrument(
            eq("AAPL"), eq("Apple"), eq(InstrumentType.STOCK), eq(Exchange.NASDAQ)))
        .thenReturn(instrument);

    mockMvc
        .perform(
            post("/api/v1/admin/instruments")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "symbol": "AAPL",
                      "name": "Apple",
                      "type": "STOCK",
                      "exchange": "NASDAQ"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").value(42));
  }

  @Test
  void createInstrument_missingSymbol_returnsBadRequest() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/instruments")
                .contentType(APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Apple",
                      "type": "STOCK",
                      "exchange": "NASDAQ"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
  }
}
