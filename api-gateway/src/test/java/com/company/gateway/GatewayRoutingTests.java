package com.company.gateway;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTests {

    static MockWebServer financeMock;
    static MockWebServer marketMock;

    @Autowired
    WebTestClient webTestClient;

    @BeforeAll
    static void setup() throws IOException {
        financeMock = new MockWebServer();
        financeMock.start();
        marketMock = new MockWebServer();
        marketMock.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        financeMock.shutdown();
        marketMock.shutdown();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("gateway.services.finance-base-uri", () -> financeMock.url("/").toString());
        r.add("gateway.services.market-base-uri", () -> marketMock.url("/").toString());
    }

    @Test
    void api_should_route_to_finance_and_remove_spoofed_user_header() throws Exception {
        financeMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.get()
                .uri("/api/test")
                .header("X-USER-ID", "00000000-0000-0000-0000-000000000000")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Correlation-Id");

        var recorded = financeMock.takeRequest();
        Assertions.assertEquals("/api/test", recorded.getPath());

        // spoof header silinmiş olmalı
        Assertions.assertNull(recorded.getHeader("X-USER-ID"));
    }

    @Test
    void market_should_rewrite_path() throws Exception {
        marketMock.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}")
                .addHeader("Content-Type", "application/json"));

        webTestClient.get()
                .uri("/market/crypto/latest")
                .exchange()
                .expectStatus().isOk();

        var recorded = marketMock.takeRequest();
        Assertions.assertEquals("/api/market/crypto/latest", recorded.getPath());
    }
}