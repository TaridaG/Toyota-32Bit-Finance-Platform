package com.company.marketdataservice.provider.investing;

import com.company.marketdataservice.config.InvestingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvestingStockPriceProviderTest {

    private MockWebServer server;
    private InvestingStockPriceProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        InvestingProperties props = new InvestingProperties();
        props.setBaseUrl("http://127.0.0.1:" + server.getPort());
        props.setPricePathTemplate("/api/financialdata/{symbol}/price");
        props.setUserAgent("test-agent");
        WebClient wc = WebClient.builder().baseUrl("http://127.0.0.1:" + server.getPort()).build();
        provider = new InvestingStockPriceProvider(props, new ObjectMapper(), wc);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    void fetchPrice_readsLastField() {
        server.enqueue(new MockResponse()
                .setBody("{\"last\":\"12.34\"}")
                .addHeader("Content-Type", "application/json"));

        BigDecimal price = provider.fetchPrice("GARAN");

        assertEquals(new BigDecimal("12.34"), price);
    }

    @Test
    void fetchPrice_throwsOnEmptyBody() {
        server.enqueue(new MockResponse().setBody("").addHeader("Content-Type", "application/json"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> provider.fetchPrice("THYAO"));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void extractPrice_emptyObject() {
        assertTrue(InvestingStockPriceProvider.extractPrice(JsonNodeFactory.instance.objectNode()).isEmpty());
    }
}
