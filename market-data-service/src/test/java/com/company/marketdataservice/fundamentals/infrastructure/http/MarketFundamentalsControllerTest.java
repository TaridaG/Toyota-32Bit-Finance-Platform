package com.company.marketdataservice.fundamentals.infrastructure.http;

import com.company.marketdataservice.fundamentals.application.InstrumentFundamentalsService;
import com.company.marketdataservice.fundamentals.infrastructure.http.dto.InstrumentFundamentalsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketFundamentalsControllerTest {

    @Mock
    private InstrumentFundamentalsService instrumentFundamentalsService;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(
                new MarketFundamentalsController(instrumentFundamentalsService)
        ).build();
    }

    @Test
    void fundamentals_returnsDtoForSymbol() {
        InstrumentFundamentalsDto dto = new InstrumentFundamentalsDto(
                "AAPL",
                "FINNHUB",
                "AAPL",
                "Apple Inc.",
                "US",
                "USD",
                "NASDAQ",
                "1980-12-12",
                "Technology",
                "https://apple.com",
                null,
                null,
                null,
                null,
                Instant.parse("2026-05-23T10:00:00Z"),
                false,
                List.of()
        );
        when(instrumentFundamentalsService.getFundamentals("AAPL", false)).thenReturn(dto);

        webTestClient.get()
                .uri("/api/v1/market/instruments/AAPL/fundamentals")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.symbol").isEqualTo("AAPL")
                .jsonPath("$.provider").isEqualTo("FINNHUB");

        verify(instrumentFundamentalsService).getFundamentals("AAPL", false);
    }
}
