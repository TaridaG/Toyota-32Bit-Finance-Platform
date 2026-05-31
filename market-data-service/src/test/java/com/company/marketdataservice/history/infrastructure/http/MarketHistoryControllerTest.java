package com.company.marketdataservice.history.infrastructure.http;

import com.company.marketdataservice.history.application.HistoricalMarketDataReadService;
import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketHistoryControllerTest {

    @Mock
    private HistoricalMarketDataReadService historicalMarketDataReadService;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(
                new MarketHistoryController(historicalMarketDataReadService)
        ).build();
    }

    @Test
    void priceHistory_returnsSeriesForSymbolAndRange() {
        List<HistoryPointDto> points = List.of(
                new HistoryPointDto(Instant.parse("2026-05-01T00:00:00Z"), new BigDecimal("100.50"))
        );
        when(historicalMarketDataReadService.getPriceHistory(
                "AAPL",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 7)
        )).thenReturn(points);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/market/prices/history")
                        .queryParam("symbol", "AAPL")
                        .queryParam("from", "2026-05-01")
                        .queryParam("to", "2026-05-07")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].value").isEqualTo(100.50)
                .jsonPath("$[0].time").exists();

        verify(historicalMarketDataReadService).getPriceHistory(
                "AAPL",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 7)
        );
    }
}
