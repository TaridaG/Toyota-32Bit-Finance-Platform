package com.company.gateway.bootstrap.config;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitConfigTest {

    private final KeyResolver keyResolver = new RateLimitConfig().userIdKeyResolver();

    @Test
    void userIdKeyResolver_usesTrustedUserHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/portfolio")
                        .header("X-USER-ID", "user-42")
                        .build()
        );

        assertEquals("user-42", keyResolver.resolve(exchange).block());
    }

    @Test
    void userIdKeyResolver_fallsBackToAnonymousWithoutUserHeader() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/public/login").build()
        );

        assertEquals("anonymous", keyResolver.resolve(exchange).block());
    }

    @Test
    void userIdKeyResolver_fallsBackToClientIpWhenRemoteAddressPresent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/public/login")
                .remoteAddress(new InetSocketAddress("203.0.113.10", 54321))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        assertEquals("203.0.113.10", keyResolver.resolve(exchange).block());
    }
}
