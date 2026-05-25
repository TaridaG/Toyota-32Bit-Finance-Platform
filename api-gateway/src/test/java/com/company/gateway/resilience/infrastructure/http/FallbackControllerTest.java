package com.company.gateway.resilience.infrastructure.http;

import com.company.gateway.shared.filter.CorrelationIdGlobalFilter;
import com.company.gateway.shared.web.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class FallbackControllerTest {

    private final FallbackController controller = new FallbackController();

    @Test
    void financeFallback_returns503WithCorrelationId() {
        MockServerWebExchange exchange = exchangeWithCorrelationId("corr-finance");

        ResponseEntity<ErrorResponse> response = controller.financeFallback(exchange).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("FINANCE_UNAVAILABLE", response.getBody().code());
        assertEquals("corr-finance", response.getBody().correlationId());
        assertEquals("corr-finance", response.getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID));
    }

    @Test
    void marketFallback_returns503WithErrorResponse() {
        MockServerWebExchange exchange = exchangeWithCorrelationId("corr-market");

        ResponseEntity<ErrorResponse> response = controller.marketFallback(exchange).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("MARKET_UNAVAILABLE", response.getBody().code());
        assertEquals("corr-market", response.getBody().correlationId());
    }

    @Test
    void newsFallback_returns503WithErrorResponse() {
        MockServerWebExchange exchange = exchangeWithCorrelationId("corr-news");

        ResponseEntity<ErrorResponse> response = controller.newsFallback(exchange).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("NEWS_UNAVAILABLE", response.getBody().code());
    }

    @Test
    void analyticsFallback_returns503WithErrorResponse() {
        MockServerWebExchange exchange = exchangeWithCorrelationId("corr-analytics");

        ResponseEntity<ErrorResponse> response = controller.analyticsFallback(exchange).block();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("ANALYTICS_UNAVAILABLE", response.getBody().code());
    }

    @Test
    void fallback_generatesCorrelationIdWhenMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/fallback/finance").build()
        );

        ResponseEntity<ErrorResponse> response = controller.financeFallback(exchange).block();

        assertNotNull(response.getBody().correlationId());
        assertEquals(response.getBody().correlationId(), response.getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID));
    }

    private static MockServerWebExchange exchangeWithCorrelationId(String correlationId) {
        return MockServerWebExchange.from(
                MockServerHttpRequest.get("/fallback/finance")
                        .header(CorrelationIdGlobalFilter.CORRELATION_ID, correlationId)
                        .build()
        );
    }
}
