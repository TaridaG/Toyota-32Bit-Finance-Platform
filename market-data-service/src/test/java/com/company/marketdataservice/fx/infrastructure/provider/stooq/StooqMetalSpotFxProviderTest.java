package com.company.marketdataservice.fx.infrastructure.provider.stooq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.company.marketdataservice.fx.domain.FxSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class StooqMetalSpotFxProviderTest {

    private MockWebServer server;
    private StooqMetalSpotFxProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        String base = "http://127.0.0.1:" + server.getPort();
        provider = new StooqMetalSpotFxProvider(
                WebClient.create(),
                new ObjectMapper(),
                base + "/api/prices.json",
                base + "/q/l/?s=%s&i=d"
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    void mapsMintedMetalSpotPayloadIntoTrySnapshots() {
        server.enqueue(new MockResponse()
                .setBody("""
                        {
                          "updatedAt": "2026-05-25T13:57:48Z",
                          "metals": {
                            "gold": { "price": 4506.15, "fixedAt": "2026-05-22T15:00:00Z" },
                            "silver": { "price": 75.84, "fixedAt": "2026-05-22T12:00:00Z" },
                            "platinum": { "price": 1938, "fixedAt": "2026-05-22T14:00:00Z" },
                            "palladium": { "price": 1368, "fixedAt": "2026-05-22T14:00:00Z" }
                          }
                        }
                        """)
                .addHeader("Content-Type", "application/json"));
        server.enqueue(new MockResponse()
                .setBody("XCUUSD,20260525,000000,0,0,0,N/D")
                .addHeader("Content-Type", "text/plain"));

        List<FxSnapshot> snapshots =
                provider.fetchLatestRates(new BigDecimal("45.672300"), Instant.parse("2026-05-25T20:45:37Z"));

        assertEquals(4, snapshots.size());
        FxSnapshot gold = snapshots.stream()
                .filter(snapshot -> "XAUTRY".equals(snapshot.canonicalSymbol()))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("205806.234645"), gold.mid());
        assertEquals("MINTED_METAL_LBMA", gold.source());
        assertEquals(Instant.parse("2026-05-22T15:00:00Z"), gold.timestamp());
    }

    @Test
    void returnsEmptyWhenUsdTryMissing() {
        assertTrue(provider.fetchLatestRates(null, Instant.now()).isEmpty());
        assertTrue(provider.fetchLatestRates(BigDecimal.ZERO, Instant.now()).isEmpty());
    }
}
