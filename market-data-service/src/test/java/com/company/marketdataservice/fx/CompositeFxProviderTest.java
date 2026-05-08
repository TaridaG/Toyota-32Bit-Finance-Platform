package com.company.marketdataservice.fx;

import com.company.marketdataservice.config.FxMarketProperties;
import com.company.marketdataservice.fx.exchangerate.ExchangeRateApiFxProvider;
import com.company.marketdataservice.fx.stooq.StooqMetalSpotFxProvider;
import com.company.marketdataservice.fx.tcmb.TcmbFxProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompositeFxProviderTest {

    @Test
    void usesExchangeWhenTcmbEmpty() {
        TcmbFxProvider tcmb = mock(TcmbFxProvider.class);
        when(tcmb.fetchLatestRates()).thenReturn(List.of());
        when(tcmb.source()).thenReturn("TCMB");

        ExchangeRateApiFxProvider api = mock(ExchangeRateApiFxProvider.class);
        FxSnapshot snap = new FxSnapshot(
                "USDTRY",
                "USD",
                "TRY",
                BigDecimal.TEN,
                BigDecimal.TEN,
                BigDecimal.TEN,
                Instant.now(),
                "EXCHANGE_RATE_API"
        );
        when(api.fetchLatestRates()).thenReturn(List.of(snap));
        when(api.source()).thenReturn("EXCHANGE_RATE_API");

        FxMarketProperties props = new FxMarketProperties();
        props.setProviderOrder(List.of("TCMB", "EXCHANGE_API"));
        StooqMetalSpotFxProvider stooq = mock(StooqMetalSpotFxProvider.class);
        when(stooq.fetchLatestRates()).thenReturn(List.of());

        CompositeFxProvider composite = new CompositeFxProvider(tcmb, api, stooq, props);

        assertEquals(List.of(snap), composite.fetchLatestRates());
    }

    @Test
    void usesTcmbWhenNonEmpty() {
        TcmbFxProvider tcmb = mock(TcmbFxProvider.class);
        FxSnapshot tcmbSnap = new FxSnapshot(
                "USDTRY",
                "USD",
                "TRY",
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                Instant.now(),
                "TCMB"
        );
        when(tcmb.fetchLatestRates()).thenReturn(List.of(tcmbSnap));

        ExchangeRateApiFxProvider api = mock(ExchangeRateApiFxProvider.class);
        FxMarketProperties props = new FxMarketProperties();
        props.setProviderOrder(List.of("TCMB", "EXCHANGE_API"));
        StooqMetalSpotFxProvider stooq = mock(StooqMetalSpotFxProvider.class);
        when(stooq.fetchLatestRates()).thenReturn(List.of());

        CompositeFxProvider composite = new CompositeFxProvider(tcmb, api, stooq, props);

        assertEquals(List.of(tcmbSnap), composite.fetchLatestRates());
    }
}
