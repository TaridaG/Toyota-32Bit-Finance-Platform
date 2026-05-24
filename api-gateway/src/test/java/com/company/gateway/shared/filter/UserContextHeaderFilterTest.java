package com.company.gateway.shared.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserContextHeaderFilterTest {

    private UserContextHeaderFilter filter;

    @BeforeEach
    void setUp() {
        filter = new UserContextHeaderFilter("sub", "roles", "preferred_username", "realm_access.roles", "email");
    }

    @Test
    void filter_stripsSpoofedHeadersWhenUnauthenticated() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/market/prices")
                        .header(UserContextHeaderFilter.HDR_USER_ID, "spoof-id")
                        .header(UserContextHeaderFilter.HDR_USERNAME, "spoof-name")
                        .header(UserContextHeaderFilter.HDR_USER_ROLES, "ADMIN")
                        .header(UserContextHeaderFilter.HDR_USER_EMAIL, "evil@test.com")
                        .build()
        );
        AtomicReference<String> userId = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            userId.set(ex.getRequest().getHeaders().getFirst(UserContextHeaderFilter.HDR_USER_ID));
            return Mono.empty();
        }).block();

        assertNull(userId.get());
    }

    @Test
    void filter_injectsTrustedHeadersFromJwt() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("jwt-sub-1")
                .claim("preferred_username", "jwt-user")
                .claim("realm_access", Map.of("roles", List.of("USER", "ADMIN")))
                .claim("email", "user@example.com")
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        ServerWebExchange exchange = MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/portfolio")
                                .header(UserContextHeaderFilter.HDR_USER_ID, "spoof-id")
                                .build())
                .mutate()
                .principal(Mono.just(auth))
                .build();

        AtomicReference<String> userId = new AtomicReference<>();
        AtomicReference<String> username = new AtomicReference<>();
        AtomicReference<String> roles = new AtomicReference<>();
        AtomicReference<String> email = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            userId.set(ex.getRequest().getHeaders().getFirst(UserContextHeaderFilter.HDR_USER_ID));
            username.set(ex.getRequest().getHeaders().getFirst(UserContextHeaderFilter.HDR_USERNAME));
            roles.set(ex.getRequest().getHeaders().getFirst(UserContextHeaderFilter.HDR_USER_ROLES));
            email.set(ex.getRequest().getHeaders().getFirst(UserContextHeaderFilter.HDR_USER_EMAIL));
            return Mono.empty();
        }).block();

        assertEquals("jwt-sub-1", userId.get());
        assertEquals("jwt-user", username.get());
        assertEquals("USER,ADMIN", roles.get());
        assertEquals("user@example.com", email.get());
    }

    @Test
    void filter_usesSubAsUsernameWhenPreferredUsernameMissing() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("only-sub")
                .claim("realm_access", Map.of("roles", List.of("USER")))
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        ServerWebExchange exchange = MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/users/me").build())
                .mutate()
                .principal(Mono.just(auth))
                .build();

        AtomicReference<String> username = new AtomicReference<>();

        filter.filter(exchange, ex -> {
            username.set(ex.getRequest().getHeaders().getFirst(UserContextHeaderFilter.HDR_USERNAME));
            return Mono.empty();
        }).block();

        assertEquals("only-sub", username.get());
    }

    @Test
    void filter_omitsEmailHeaderWhenClaimBlank() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("jwt-sub-2")
                .claim("preferred_username", "jwt-user-2")
                .claim("realm_access", Map.of("roles", List.of("USER")))
                .build();
        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);

        ServerWebExchange exchange = MockServerWebExchange.from(
                        MockServerHttpRequest.get("/api/portfolio").build())
                .mutate()
                .principal(Mono.just(auth))
                .build();

        AtomicReference<String> email = new AtomicReference<>("present");

        filter.filter(exchange, ex -> {
            email.set(ex.getRequest().getHeaders().getFirst(UserContextHeaderFilter.HDR_USER_EMAIL));
            return Mono.empty();
        }).block();

        assertNull(email.get());
    }
}
