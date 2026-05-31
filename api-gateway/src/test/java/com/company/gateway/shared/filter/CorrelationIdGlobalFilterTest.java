package com.company.gateway.shared.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CorrelationIdGlobalFilterTest {

    private final CorrelationIdGlobalFilter filter = new CorrelationIdGlobalFilter();

    @Test
    void filter_preservesIncomingCorrelationId() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/market/prices")
                        .header(CorrelationIdGlobalFilter.CORRELATION_ID, "corr-existing")
                        .build()
        );
        AtomicReference<String> forwarded = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            forwarded.set(ex.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID));
            return Mono.empty();
        }).block();

        assertEquals("corr-existing", forwarded.get());
        assertEquals("corr-existing", exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID));
    }

    @Test
    void filter_generatesCorrelationIdWhenMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/news").build()
        );
        AtomicReference<String> forwarded = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            forwarded.set(ex.getRequest().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID));
            return Mono.empty();
        }).block();

        assertNotNull(forwarded.get());
        assertEquals(forwarded.get(), exchange.getResponse().getHeaders().getFirst(CorrelationIdGlobalFilter.CORRELATION_ID));
    }
}
