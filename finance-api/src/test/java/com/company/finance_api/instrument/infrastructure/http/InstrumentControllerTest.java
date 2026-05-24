package com.company.finance_api.instrument.infrastructure.http;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.Exchange;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.service.InstrumentService;
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

@WebMvcTest(controllers = InstrumentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcTestSecuritySupport.class})
@ActiveProfiles("test")
class InstrumentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private InstrumentService instrumentService;

  @Test
  void getAllInstruments_returnsInstrumentList() throws Exception {
    Instrument instrument =
        new Instrument("AAPL", "Apple", InstrumentType.STOCK, Exchange.NASDAQ);
    when(instrumentService.getAllActive()).thenReturn(List.of(instrument));

    mockMvc
        .perform(get("/api/instruments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].symbol").value("AAPL"));
  }
}
