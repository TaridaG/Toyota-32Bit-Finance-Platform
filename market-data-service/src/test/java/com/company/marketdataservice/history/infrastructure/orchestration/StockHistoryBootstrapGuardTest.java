package com.company.marketdataservice.history.infrastructure.orchestration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.company.marketdataservice.bootstrap.config.MarketHistoryBackfillProperties;
import com.company.marketdataservice.catalog.application.InstrumentIngestScopeService;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockHistoryBootstrapGuardTest {

    @Mock
    private MarketPriceHistoryRepository marketPriceHistoryRepository;
    @Mock
    private HistoricalBackfillService historicalBackfillService;
    @Mock
    private InstrumentIngestScopeService ingestScope;

    @Test
    void allowsLiveImmediatelyWhenGateDisabled() {
        MarketHistoryBackfillProperties props = enabledProps(false);
        StockHistoryBootstrapGuard guard =
                new StockHistoryBootstrapGuard(
                        marketPriceHistoryRepository,
                        historicalBackfillService,
                        props,
                        ingestScope);
        when(marketPriceHistoryRepository.countDistinctDaysBySymbol("AAPL")).thenReturn(1L);

        assertTrue(guard.isLiveAllowed("AAPL"));
    }

    @Test
    void blocksLiveWhenGateEnabledAndHistoryMissing() {
        MarketHistoryBackfillProperties props = enabledProps(true);
        StockHistoryBootstrapGuard guard =
                new StockHistoryBootstrapGuard(
                        marketPriceHistoryRepository,
                        historicalBackfillService,
                        props,
                        ingestScope);
        when(marketPriceHistoryRepository.countDistinctDaysBySymbol("AAPL")).thenReturn(1L);

        assertFalse(guard.isLiveAllowed("AAPL"));
    }

    private static MarketHistoryBackfillProperties enabledProps(boolean gateLive) {
        MarketHistoryBackfillProperties props = new MarketHistoryBackfillProperties();
        props.setEnabled(true);
        props.setGateLiveUntilHistoryReady(gateLive);
        props.setStockBootstrapParallelism(2);
        props.setMinPriceHistoryDays(900);
        return props;
    }
}
