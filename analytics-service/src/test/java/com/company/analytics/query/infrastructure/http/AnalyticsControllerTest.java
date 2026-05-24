package com.company.analytics.query.infrastructure.http;

import com.company.analytics.query.application.AnalyticsQueryService;
import com.company.analytics.shared.web.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsQueryService analyticsQueryService;

    @Test
    void getCandles_returnsSuccessEnvelope() throws Exception {
        when(analyticsQueryService.getCandles("BTCUSDT", null, null)).thenReturn(List.of());

        mockMvc.perform(get("/api/analytics/instruments/BTCUSDT/candles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getCandles_invalidInterval_returnsBadRequestEnvelope() throws Exception {
        mockMvc.perform(get("/api/analytics/instruments/BTCUSDT/candles")
                        .param("interval", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }
}
