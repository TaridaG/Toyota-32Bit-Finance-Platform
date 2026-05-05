package com.company.gateway;

import com.company.gateway.support.GatewayOidcIssuerStub;
import com.company.gateway.support.NoopRedisRateLimiterTestConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(NoopRedisRateLimiterTestConfig.class)
class GatewaySecurityTests {

    static MockWebServer newsMock;
    static MockWebServer marketMock;
    static MockWebServer financeMock;

    @Autowired
    WebTestClient webTestClient;

    private static synchronized void ensureMocksStarted() throws IOException {
        if (newsMock == null) {
            newsMock = new MockWebServer();
            newsMock.start();
            marketMock = new MockWebServer();
            marketMock.start();
            financeMock = new MockWebServer();
            financeMock.start();
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (newsMock != null) {
            newsMock.shutdown();
            marketMock.shutdown();
            financeMock.shutdown();
            newsMock = null;
            marketMock = null;
            financeMock = null;
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        ensureMocksStarted();
        GatewayOidcIssuerStub.registerIssuerUri(r);
        r.add("spring.cloud.gateway.forwarded.enabled", () -> "false");
        r.add("spring.cloud.gateway.x-forwarded.enabled", () -> "false");
        r.add("gateway.services.news-base-uri", () -> "http://127.0.0.1:" + newsMock.getPort());
        r.add("gateway.services.market-base-uri", () -> "http://127.0.0.1:" + marketMock.getPort());
        r.add("gateway.services.finance-base-uri", () -> "http://127.0.0.1:" + financeMock.getPort());
    }

    @Test
    void actuator_health_should_be_public() {
        webTestClient.get()
                .uri("/actuator/health")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void public_register_should_be_permitted_without_token() {
        financeMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"success\":true}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.post()
                .uri("/api/public/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "email", "newuser@example.com",
                        "username", "newuser",
                        "password", "password12x"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\"success\":true}");
    }

    @Test
    void public_register_should_ignore_invalid_bearer_token() {
        financeMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"success\":true}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.post()
                .uri("/api/public/register")
                .headers(h -> h.setBearerAuth("not.a.valid.jwt.token"))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "email", "newuser2@example.com",
                        "username", "newuser2",
                        "password", "password12x"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\"success\":true}");
    }

    @Test
    void public_login_should_be_permitted_without_token() {
        financeMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"success\":true}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.post()
                .uri("/api/public/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "username", "user1",
                        "password", "123456"
                ))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\"success\":true}");
    }

    @Test
    void market_should_be_public_without_token() {
        marketMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.get()
                .uri("/market/crypto/latest")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\"ok\":true}");
    }

    @Test
    void api_news_should_be_public_without_token() {
        newsMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.get()
                .uri("/api/news/headlines")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\"ok\":true}");
    }

    @Test
    void news_admin_ingest_should_return_401_without_token() {
        webTestClient.post()
                .uri("/api/news/admin/ingest")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void news_admin_ingest_should_return_403_for_user_role() throws Exception {
        String token = GatewayOidcIssuerStub.mintAccessToken(b -> b
                .subject("user-123")
                .claim("realm_access", Map.of("roles", List.of("USER"))));

        webTestClient.post()
                .uri("/api/news/admin/ingest")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void news_admin_ingest_should_allow_admin_role() throws Exception {
        newsMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}")
                .addHeader("Content-Type", "application/json"));

        String token = GatewayOidcIssuerStub.mintAccessToken(b -> b
                .subject("admin-123")
                .claim("realm_access", Map.of("roles", List.of("ADMIN"))));

        webTestClient.post()
                .uri("/api/news/admin/ingest")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\"ok\":true}");

        RecordedRequest recorded = newsMock.takeRequest();
        Assertions.assertEquals("/api/news/admin/ingest", recorded.getPath());
        Assertions.assertEquals("POST", recorded.getMethod());
    }

    @Test
    void should_return_401_without_token_for_protected_api() {
        webTestClient.get()
                .uri("/api/admin/stats")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void should_allow_authenticated_user_for_api() throws Exception {
        financeMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}")
                .addHeader("Content-Type", "application/json"));

        String token = GatewayOidcIssuerStub.mintAccessToken(b -> b
                .subject("user-123")
                .claim("realm_access", Map.of("roles", List.of("USER"))));

        webTestClient.get()
                .uri("/api/instruments")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\"ok\":true}");
    }

    @Test
    void should_forbid_admin_path_for_user() throws Exception {
        String token = GatewayOidcIssuerStub.mintAccessToken(b -> b
                .subject("user-123")
                .claim("realm_access", Map.of("roles", List.of("USER"))));

        webTestClient.get()
                .uri("/api/admin/stats")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void should_allow_admin_path_for_admin() throws Exception {
        financeMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"admin\":true}")
                .addHeader("Content-Type", "application/json"));

        String token = GatewayOidcIssuerStub.mintAccessToken(b -> b
                .subject("admin-123")
                .claim("realm_access", Map.of("roles", List.of("ADMIN"))));

        webTestClient.get()
                .uri("/api/admin/stats")
                .headers(h -> h.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\"admin\":true}");
    }
}
