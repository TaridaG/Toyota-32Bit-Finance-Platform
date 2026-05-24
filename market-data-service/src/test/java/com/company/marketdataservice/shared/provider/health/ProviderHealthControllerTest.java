package com.company.marketdataservice.shared.provider.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderHealthControllerTest {

    @Mock
    private ProviderHealthTracker tracker;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToController(new ProviderHealthController(tracker)).build();
    }

    @Test
    void getMetrics_returnsProviderMetrics() {
        ProviderHealthTracker.ProviderMetrics metrics = new ProviderHealthTracker.ProviderMetrics();
        metrics.getSuccess().set(12);
        metrics.getFailure().set(1);
        when(tracker.getMetrics("FINNHUB")).thenReturn(metrics);

        webTestClient.get()
                .uri("/api/admin/providers/FINNHUB")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(12)
                .jsonPath("$.failure").isEqualTo(1);

        verify(tracker).getMetrics("FINNHUB");
    }
}
