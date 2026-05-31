package com.company.marketdataservice.history.infrastructure.http;

import com.company.marketdataservice.history.infrastructure.http.dto.IngestionStatusResponseDto;
import com.company.marketdataservice.history.infrastructure.orchestration.IngestionStatusQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngestionStatusControllerTest {

    @Mock
    private IngestionStatusQueryService ingestionStatusQueryService;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(
                new IngestionStatusController(ingestionStatusQueryService)
        ).build();
    }

    @Test
    void status_returnsFilteredIngestionSnapshot() {
        IngestionStatusResponseDto response = new IngestionStatusResponseDto(
                Instant.parse("2026-05-23T10:00:00Z"),
                Map.of("RUNNING", 2L),
                List.of()
        );
        when(ingestionStatusQueryService.getStatus("STOCK", "RUNNING", "AAP")).thenReturn(response);

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/market/ingestion/status")
                        .queryParam("assetType", "STOCK")
                        .queryParam("status", "RUNNING")
                        .queryParam("symbolPrefix", "AAP")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.summary.RUNNING").isEqualTo(2);

        verify(ingestionStatusQueryService).getStatus("STOCK", "RUNNING", "AAP");
    }
}
