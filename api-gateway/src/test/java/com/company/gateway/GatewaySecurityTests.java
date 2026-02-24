package com.company.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewaySecurityTests {

    @Autowired
    WebTestClient webTestClient;

    @Test
    void should_return_401_without_token() {
        webTestClient.get()
                .uri("/api/instruments")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void should_allow_authenticated_user_for_api() {
        webTestClient.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-123"))
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                .get()
                .uri("/api/instruments")
                .exchange()
                // upstream mocklamıyorsan 502 görebilirsin; burada amaç security gate.
                .expectStatus().is5xxServerError();
    }

    @Test
    void should_forbid_admin_path_for_user() {
        webTestClient.mutateWith(mockJwt().jwt(jwt -> jwt.subject("user-123"))
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")))
                .get()
                .uri("/api/admin/stats")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void should_allow_admin_path_for_admin() {
        webTestClient.mutateWith(mockJwt().jwt(jwt -> jwt.subject("admin-123"))
                        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")))
                .get()
                .uri("/api/admin/stats")
                .exchange()
                .expectStatus().is5xxServerError();
    }
}