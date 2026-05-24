package com.company.marketdataservice.spot.infrastructure.http;

import com.company.marketdataservice.spot.application.MarketDataReadService;
import com.company.marketdataservice.spot.application.MarketSegmentPulseService;
import com.company.marketdataservice.spot.infrastructure.http.dto.FxRateDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketSegmentPulseOverallDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketSegmentPulseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataControllerTest {

    @Mock
    private MarketDataReadService marketDataReadService;
    @Mock
    private MarketSegmentPulseService marketSegmentPulseService;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        MarketDataController controller = new MarketDataController(marketDataReadService, marketSegmentPulseService);
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void prices_returnsSegmentFilteredSnapshot() {
        MarketPriceDto price = MarketPriceDto.basic(
                "BTCUSDT",
                new BigDecimal("65000.12"),
                "BINANCE",
                Instant.parse("2026-05-23T10:00:00Z")
        );
        when(marketDataReadService.getLatestPrices("crypto")).thenReturn(List.of(price));

        webTestClient.get()
                .uri("/api/market/prices?segment=crypto")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].symbol").isEqualTo("BTCUSDT")
                .jsonPath("$[0].price").isEqualTo(65000.12);

        verify(marketDataReadService).getLatestPrices("crypto");
    }

    @Test
    void segmentPulse_returnsAggregatedPulse() {
        when(marketSegmentPulseService.getPulse()).thenReturn(new MarketSegmentPulseResponse(
                new MarketSegmentPulseOverallDto(1.2, 10, 6),
                List.of(),
                Instant.parse("2026-05-23T10:00:00Z")
        ));

        webTestClient.get()
                .uri("/api/market/segments/pulse")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.overall.count").isEqualTo(10)
                .jsonPath("$.overall.advancingCount").isEqualTo(6);
    }

    @Test
    void fx_returnsLatestRates() {
        when(marketDataReadService.getFxRates()).thenReturn(List.of(
                new FxRateDto(
                        "USDTRY",
                        new BigDecimal("32.00"),
                        new BigDecimal("32.20"),
                        new BigDecimal("32.10"),
                        "TCMB",
                        Instant.parse("2026-05-23T09:00:00Z")
                )
        ));

        webTestClient.get()
                .uri("/api/market/fx")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].symbol").isEqualTo("USDTRY");
    }
}
