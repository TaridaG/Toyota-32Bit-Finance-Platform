package com.company.marketdataservice.fx.infrastructure.provider.exchangerate;
import com.company.marketdataservice.bootstrap.config.FxMarketProperties;
import com.company.marketdataservice.fx.domain.FxSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExchangeRateApiFxProviderTest {

    private MockWebServer server;
    private ExchangeRateApiFxProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        FxMarketProperties props = new FxMarketProperties();
        props.setExchangeRateUrl("http://127.0.0.1:" + server.getPort() + "/v6/latest/TRY");
        props.setProviderCurrencies(List.of("USD", "EUR"));
        WebClient wc = WebClient.create();
        provider = new ExchangeRateApiFxProvider(props, new ObjectMapper(), wc);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    void mapsUsdtryFromTryBaseRates() {
        server.enqueue(new MockResponse()
                .setBody("""
                        {"result":"success","base_code":"TRY","time_last_update_unix":1700000000,
                        "rates":{"USD":0.02,"EUR":0.018}}
                        """)
                .addHeader("Content-Type", "application/json"));

        List<FxSnapshot> snaps = provider.fetchLatestRates();

        assertEquals(2, snaps.size());
        FxSnapshot usd = snaps.stream().filter(s -> "USDTRY".equals(s.canonicalSymbol())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("50.000000"), usd.mid());
        assertEquals("EXCHANGE_RATE_API", usd.source());
    }

    @Test
    void returnsEmptyOnNonSuccess() {
        server.enqueue(new MockResponse().setBody("{\"result\":\"error\"}").addHeader("Content-Type", "application/json"));

        assertTrue(provider.fetchLatestRates().isEmpty());
    }
}
