package com.company.marketdataservice.spot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.company.marketdataservice.bootstrap.config.HotReadCacheProperties;
import com.company.marketdataservice.bootstrap.config.TcmbBondMarketProperties;
import com.company.marketdataservice.shared.cache.JsonCacheService;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.FxRateHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.shared.provider.tcmb.TcmbBondEvdsClient;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceDto;
import com.company.marketdataservice.spot.infrastructure.snapshot.MarketSnapshotStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketDataReadServiceImplCryptoFallbackTest {

    @Mock
    private MarketSnapshotStore snapshotStore;

    @Mock
    private MarketPriceHistoryRepository marketPriceHistoryRepository;

    @Mock
    private FxRateHistoryRepository fxRateHistoryRepository;

    @Mock
    private FundNavHistoryRepository fundNavHistoryRepository;

    @Mock
    private TcmbBondMarketProperties bondMarketProperties;

    @Mock
    private TcmbBondEvdsClient tcmbBondEvdsClient;

    @Mock
    private JsonCacheService jsonCacheService;

    @Test
    void getLatestPrices_mergesCryptoFromDbWhenSnapshotWarm() {
        HotReadCacheProperties hotReadCacheProperties = new HotReadCacheProperties();
        hotReadCacheProperties.setEnabled(false);
        when(snapshotStore.listPrices())
                .thenReturn(List.of(MarketPriceDto.basic("GARAN", new BigDecimal("410"), "YAHOO", Instant.now())));
        when(snapshotStore.listFunds()).thenReturn(List.of());
        when(fundNavHistoryRepository.findLatestRowPerFundCode()).thenReturn(List.of());
        when(marketPriceHistoryRepository.findLatestPricesPerSymbol())
                .thenReturn(List.of(historyRow("BTCUSDT", "95000", "BINANCE")));

        MarketDataReadServiceImpl svc =
                new MarketDataReadServiceImpl(
                        snapshotStore,
                        marketPriceHistoryRepository,
                        fxRateHistoryRepository,
                        fundNavHistoryRepository,
                        bondMarketProperties,
                        tcmbBondEvdsClient,
                        jsonCacheService,
                        hotReadCacheProperties);

        List<MarketPriceDto> crypto = svc.getLatestPrices("crypto");
        assertEquals(1, crypto.size());
        assertEquals("BTCUSDT", crypto.get(0).symbol());
        assertTrue(crypto.get(0).price().compareTo(new BigDecimal("95000")) == 0);
    }

    @Test
    void getLatestPrices_mergesBistFromDbWhenSnapshotWarmAndSymbolMissing() {
        HotReadCacheProperties hotReadCacheProperties = new HotReadCacheProperties();
        hotReadCacheProperties.setEnabled(false);
        when(snapshotStore.listPrices())
                .thenReturn(List.of(MarketPriceDto.basic("GARAN", new BigDecimal("410"), "YAHOO", Instant.now())));
        when(snapshotStore.listFunds()).thenReturn(List.of());
        when(fundNavHistoryRepository.findLatestRowPerFundCode()).thenReturn(List.of());
        when(marketPriceHistoryRepository.findLatestPricesPerSymbol())
                .thenReturn(List.of(historyRow("AKBNK", "82.40", "YAHOO")));

        MarketDataReadServiceImpl svc =
                new MarketDataReadServiceImpl(
                        snapshotStore,
                        marketPriceHistoryRepository,
                        fxRateHistoryRepository,
                        fundNavHistoryRepository,
                        bondMarketProperties,
                        tcmbBondEvdsClient,
                        jsonCacheService,
                        hotReadCacheProperties);

        List<MarketPriceDto> all = svc.getLatestPrices(null);
        MarketPriceDto akbnk =
                all.stream().filter(p -> "AKBNK".equals(p.symbol())).findFirst().orElseThrow();
        assertTrue(akbnk.price().compareTo(new BigDecimal("82.40")) == 0);

        MarketPriceDto garan =
                all.stream().filter(p -> "GARAN".equals(p.symbol())).findFirst().orElseThrow();
        assertTrue(garan.price().compareTo(new BigDecimal("410")) == 0);
    }

    private static MarketPriceHistoryRepository.LatestMarketPriceView historyRow(
            String symbol, String price, String source) {
        return new MarketPriceHistoryRepository.LatestMarketPriceView() {
            @Override
            public String getSymbol() {
                return symbol;
            }

            @Override
            public BigDecimal getPrice() {
                return new BigDecimal(price);
            }

            @Override
            public String getSource() {
                return source;
            }

            @Override
            public Instant getTimestamp() {
                return Instant.parse("2026-05-24T02:51:10Z");
            }
        };
    }
}
