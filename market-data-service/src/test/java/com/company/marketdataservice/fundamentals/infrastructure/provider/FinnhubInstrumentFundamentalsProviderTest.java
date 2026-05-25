package com.company.marketdataservice.fundamentals.infrastructure.provider;

import com.company.marketdataservice.bootstrap.config.FinnhubProperties;
import com.company.marketdataservice.catalog.infrastructure.persistence.InstrumentCatalogEntry;
import com.company.marketdataservice.spot.infrastructure.provider.finnhub.FinnhubClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class FinnhubInstrumentFundamentalsProviderTest {

    @Mock
    private FinnhubClient finnhubClient;

    private FinnhubProperties finnhubProperties;
    private FinnhubInstrumentFundamentalsProvider provider;

    @BeforeEach
    void setUp() {
        finnhubProperties = new FinnhubProperties();
        finnhubProperties.setEnabled(true);
        finnhubProperties.setSymbols(List.of("AAPL", "NVDA", "VOO"));
        provider = new FinnhubInstrumentFundamentalsProvider(finnhubClient, finnhubProperties);
    }

    @Test
    void supportsFinnhubConfiguredUsSymbols() {
        assertThat(provider.supports(stock("AAPL"))).isTrue();
        assertThat(provider.supports(stock("nvda"))).isTrue();
    }

    @Test
    void doesNotSupportBistSymbols() {
        assertThat(provider.supports(stock("AKBNK"))).isFalse();
        assertThat(provider.supports(stock("SAHOL"))).isFalse();
    }

    @Test
    void doesNotSupportWhenFinnhubDisabled() {
        finnhubProperties.setEnabled(false);
        assertThat(provider.supports(stock("AAPL"))).isFalse();
    }

    private static InstrumentCatalogEntry stock(String symbol) {
        InstrumentCatalogEntry entry = new InstrumentCatalogEntry();
        entry.setCanonicalSymbol(symbol);
        entry.setAssetClass("STOCK");
        entry.setActive(true);
        return entry;
    }
}
