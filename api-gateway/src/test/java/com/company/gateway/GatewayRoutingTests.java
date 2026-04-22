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
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(NoopRedisRateLimiterTestConfig.class)
class GatewayRoutingTests {

    static MockWebServer financeMock;
    static MockWebServer marketMock;

    @Autowired
    WebTestClient webTestClient;

    private static synchronized void ensureMocksStarted() throws IOException {
        if (financeMock == null) {
            financeMock = new MockWebServer();
            financeMock.start();
            marketMock = new MockWebServer();
            marketMock.start();
        }
    }

    @AfterAll
    static void tearDown() throws IOException {
        if (financeMock != null) {
            financeMock.shutdown();
            marketMock.shutdown();
            financeMock = null;
            marketMock = null;
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws Exception {
        ensureMocksStarted();
        GatewayOidcIssuerStub.registerIssuerUri(r);
        r.add("gateway.services.finance-base-uri", () -> "http://127.0.0.1:" + financeMock.getPort());
        r.add("gateway.services.market-base-uri", () -> "http://127.0.0.1:" + marketMock.getPort());
    }

    @Test
    void api_should_route_to_finance_and_remove_spoofed_user_header_when_authenticated() throws Exception {
        financeMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}")
                .addHeader("Content-Type", "application/json"));

        String token = GatewayOidcIssuerStub.mintAccessToken(b -> b
                .subject("jwt-sub-1")
                .claim("preferred_username", "jwt-user")
                .claim("realm_access", Map.of("roles", List.of("USER"))));

        webTestClient.get()
                .uri("/api/test")
                .headers(h -> {
                    h.setBearerAuth(token);
                    h.set("X-USER-ID", "00000000-0000-0000-0000-000000000000");
                    h.set("X-USERNAME", "spoof-name");
                    h.set("X-USER-ROLES", "ADMIN");
                    h.set("X-USER-EMAIL", "attacker@evil.test");
                })
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Correlation-Id")
                .expectBody(String.class).isEqualTo("{\"ok\":true}");

        RecordedRequest recorded = financeMock.takeRequest();
        Assertions.assertEquals("/api/test", recorded.getPath());
        Assertions.assertEquals("jwt-sub-1", recorded.getHeader("X-USER-ID"));
        Assertions.assertEquals("jwt-user", recorded.getHeader("X-USERNAME"));
        Assertions.assertNull(recorded.getHeader("X-USER-EMAIL"));
    }

    @Test
    void market_should_rewrite_path_without_authentication() throws Exception {
        marketMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.get()
                .uri("/market/crypto/latest")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("{\"ok\":true}");

        RecordedRequest recorded = marketMock.takeRequest();
        Assertions.assertEquals("/api/market/crypto/latest", recorded.getPath());
        Assertions.assertNull(recorded.getHeader("X-USER-ID"));
    }
}
