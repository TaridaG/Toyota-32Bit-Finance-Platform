package com.company.marketdataservice.fx.infrastructure.provider.evds;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.company.marketdataservice.bootstrap.config.MarketEvdsProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

class EvdsHistoricalFxProviderTest {

    @Test
    void skipsSpotMetalSymbols() {
        MarketEvdsProperties props = new MarketEvdsProperties();
        props.setApiKey("test-key");
        props.setBaseUrl("https://evds.example");
        EvdsHistoricalFxProvider provider = new EvdsHistoricalFxProvider(
                props,
                new ObjectMapper(),
                mock(WebClient.class)
        );

        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);

        assertTrue(provider.fetchRange("XAUTRY", start, end).isEmpty());
        assertTrue(provider.fetchRange("XPTTRY", start, end).isEmpty());
    }
}
