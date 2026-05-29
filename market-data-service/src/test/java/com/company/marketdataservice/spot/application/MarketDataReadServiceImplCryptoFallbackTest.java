package com.company.marketdataservice.spot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.company.marketdataservice.bootstrap.config.TcmbBondMarketProperties;
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

    @Test
    void getLatestPrices_mergesCryptoFromDbWhenSnapshotWarm() {
        when(snapshotStore.listPrices())
                .thenReturn(List.of(MarketPriceDto.basic("GARAN", new BigDecimal("410"), "YAHOO", Instant.now())));
        when(snapshotStore.listFunds()).thenReturn(List.of());
        when(fundNavHistoryRepository.findLatestRowPerFundCode()).thenReturn(List.of());
        when(marketPriceHistoryRepository.findLatestTrbondPricesPerSymbol()).thenReturn(List.of());
        when(marketPriceHistoryRepository.findLatestCryptoPricesPerSymbol())
                .thenReturn(List.of(cryptoRow("BTCUSDT", "95000")));

        MarketDataReadServiceImpl svc =
                new MarketDataReadServiceImpl(
                        snapshotStore,
                        marketPriceHistoryRepository,
                        fxRateHistoryRepository,
                        fundNavHistoryRepository,
                        bondMarketProperties,
                        tcmbBondEvdsClient);

        List<MarketPriceDto> crypto = svc.getLatestPrices("crypto");
        assertEquals(1, crypto.size());
        assertEquals("BTCUSDT", crypto.get(0).symbol());
        assertTrue(crypto.get(0).price().compareTo(new BigDecimal("95000")) == 0);
    }

    private static MarketPriceHistoryRepository.LatestMarketPriceView cryptoRow(String symbol, String price) {
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
                return "BINANCE";
            }

            @Override
            public Instant getTimestamp() {
                return Instant.parse("2026-05-24T02:51:10Z");
            }
        };
    }
}
