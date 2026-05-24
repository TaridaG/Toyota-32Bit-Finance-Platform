package com.company.notification.insight.infrastructure.http;

import com.company.notification.bootstrap.config.FinanceApiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceInstrumentLookupClientTest {

    @Mock
    private RestTemplate restTemplate;

    private FinanceApiProperties financeApiProperties;
    private FinanceInstrumentLookupClient client;

    @BeforeEach
    void setUp() {
        financeApiProperties = new FinanceApiProperties();
        financeApiProperties.setBaseUrl("http://finance-api:8080");
        client = new FinanceInstrumentLookupClient(restTemplate, financeApiProperties, new ObjectMapper());
    }

    @Test
    void resolveInstrumentId_returns_empty_for_blank_symbol() {
        assertTrue(client.resolveInstrumentId(null).isEmpty());
        assertTrue(client.resolveInstrumentId("  ").isEmpty());
    }

    @Test
    void resolveInstrumentId_returns_empty_when_base_url_missing() {
        financeApiProperties.setBaseUrl("");
        assertTrue(client.resolveInstrumentId("BTCUSDT").isEmpty());
    }

    @Test
    void resolveInstrumentId_matches_symbol_case_insensitive() {
        String json = """
                {"data":[{"id":42,"symbol":"btcusdt"},{"id":99,"symbol":"ETHUSDT"}]}
                """;
        when(restTemplate.getForObject(eq("http://finance-api:8080/api/instruments"), eq(String.class)))
                .thenReturn(json);

        assertEquals(Optional.of(42L), client.resolveInstrumentId("BTCUSDT"));
        assertEquals(Optional.of(99L), client.resolveInstrumentId("ethusdt"));
    }

    @Test
    void resolveInstrumentId_returns_empty_when_symbol_not_found() {
        String json = """
                {"data":[{"id":1,"symbol":"AAPL"}]}
                """;
        when(restTemplate.getForObject(eq("http://finance-api:8080/api/instruments"), eq(String.class)))
                .thenReturn(json);

        assertTrue(client.resolveInstrumentId("MISSING").isEmpty());
    }

    @Test
    void resolveInstrumentId_returns_empty_on_http_failure() {
        when(restTemplate.getForObject(eq("http://finance-api:8080/api/instruments"), eq(String.class)))
                .thenThrow(new RestClientException("timeout"));

        assertTrue(client.resolveInstrumentId("BTCUSDT").isEmpty());
    }

    @Test
    void resolveInstrumentId_returns_empty_on_invalid_json() {
        when(restTemplate.getForObject(eq("http://finance-api:8080/api/instruments"), eq(String.class)))
                .thenReturn("not-json");

        assertTrue(client.resolveInstrumentId("BTCUSDT").isEmpty());
    }
}
