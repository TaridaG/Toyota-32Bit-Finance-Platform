package com.company.gateway.shared.web;

import com.company.gateway.shared.filter.CorrelationIdGlobalFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalErrorWebExceptionHandlerTest {

    private GlobalErrorWebExceptionHandler handler;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        handler = new GlobalErrorWebExceptionHandler(objectMapper);
    }

    @Test
    void handle_returns502JsonWithCorrelationId() throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/market/prices")
                        .header(CorrelationIdGlobalFilter.CORRELATION_ID, "corr-502")
                        .build()
        );

        handler.handle(exchange, new RuntimeException("upstream timeout")).block();

        assertEquals(HttpStatus.BAD_GATEWAY, exchange.getResponse().getStatusCode());
        String body = exchange.getResponse().getBodyAsString().block();
        assertTrue(body.contains("GATEWAY_ERROR"));
        assertTrue(body.contains("upstream timeout"));
        assertTrue(body.contains("corr-502"));
        assertEquals("corr-502", exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID));
    }

    @Test
    void handle_generatesCorrelationIdWhenMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/portfolio").build()
        );

        handler.handle(exchange, new RuntimeException("boom")).block();

        String correlationId = exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID);
        assertNotNull(correlationId);
        assertTrue(exchange.getResponse().getBodyAsString().block().contains(correlationId));
    }

    @Test
    void handle_usesUnexpectedErrorWhenMessageNull() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/orders")
                        .header(CorrelationIdGlobalFilter.CORRELATION_ID, "corr-null-msg")
                        .build()
        );

        handler.handle(exchange, new RuntimeException((String) null)).block();

        assertTrue(exchange.getResponse().getBodyAsString().block().contains("Unexpected error"));
    }

    @Test
    void handle_propagatesWhenResponseAlreadyCommitted() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/trades").build()
        );
        exchange.getResponse().setComplete().block();
        RuntimeException error = new RuntimeException("late failure");

        RuntimeException propagated = assertThrows(RuntimeException.class,
                () -> handler.handle(exchange, error).block());
        assertEquals("late failure", propagated.getMessage());
    }
}
