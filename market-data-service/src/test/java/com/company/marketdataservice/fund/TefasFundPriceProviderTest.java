package com.company.marketdataservice.fund;

import com.company.marketdataservice.config.FundMarketProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TefasFundPriceProviderTest {

    @Test
    void fetchLatestNavs_mockResponse_returnsNav() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(
                    new MockResponse()
                            .setBody(
                                    """
                                    {
                                      "data": [
                                        {"FONKODU": "AFT", "FIYAT": 3.4567, "TARIH": 1704067200000}
                                      ]
                                    }
                                    """
                            )
                            .addHeader("Content-Type", "application/json")
            );
            server.start();

            FundMarketProperties props = new FundMarketProperties();
            props.setTefasBindHistoryUrl(server.url("/api/DB/BindHistoryInfo").toString());
            props.setTefasHistoryLookbackDays(0);
            props.setTefasHistoryChunkInclusiveDays(30);

            WebClient wc = WebClient.builder().build();
            TefasFundPriceProvider p =
                    new TefasFundPriceProvider(props, new ObjectMapper(), wc);

            List<FundSnapshot> snaps = p.fetchLatestNavs(List.of("AFT"));

            assertEquals(1, snaps.size());
            assertEquals(new BigDecimal("3.4567"), snaps.get(0).nav());
            assertEquals("AFT", snaps.get(0).fundCode());
            assertEquals("TEFAS", snaps.get(0).source());

            server.takeRequest(); // consumes POST request
            assertEquals(1, server.getRequestCount());
        }
    }

    @Test
    void fetchLatestNavs_emptyBody_returnsEmptyAndDoesNotThrow() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody(""));
            server.start();

            FundMarketProperties props = new FundMarketProperties();
            props.setTefasBindHistoryUrl(server.url("/api/DB/BindHistoryInfo").toString());
            props.setTefasHistoryLookbackDays(0);
            props.setTefasHistoryChunkInclusiveDays(7);

            TefasFundPriceProvider p =
                    new TefasFundPriceProvider(props, new ObjectMapper(), WebClient.builder().build());

            assertTrue(p.fetchLatestNavs(List.of("AFT")).isEmpty());
            server.takeRequest();
        }
    }
}
