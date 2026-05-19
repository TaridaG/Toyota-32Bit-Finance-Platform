package com.company.marketdataservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.company.marketdataservice.dto.HistoryPointDto;
import com.company.marketdataservice.dto.MarketPriceSummaryDto;
import com.company.marketdataservice.history.FundNavHistoryRepository;
import com.company.marketdataservice.history.FxRateHistoryRepository;
import com.company.marketdataservice.history.MarketPriceHistoryRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class HistoricalMarketDataReadServiceImplMetalSummaryTest {

    @Mock
    private MarketPriceHistoryRepository marketPriceHistoryRepository;

    @Mock
    private FxRateHistoryRepository fxRateHistoryRepository;

    @Mock
    private FundNavHistoryRepository fundNavHistoryRepository;

    @Test
    void metalSpotSummary_readsFromFxRateHistory_notMarketPriceHistory() {
        Instant now = Instant.parse("2026-05-16T12:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        Instant toExclusive = now.plus(1, ChronoUnit.DAYS);

        when(fxRateHistoryRepository.findLatestHistoryPoint(eq("XAUTRY"), any(Pageable.class)))
                .thenReturn(List.of(new HistoryPointDto(now, new BigDecimal("206000"))));
        when(fxRateHistoryRepository.findHistoryPoints(eq("XAUTRY"), eq(now.minus(1, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of(
                        new HistoryPointDto(now.minus(1, ChronoUnit.DAYS), new BigDecimal("200000")),
                        new HistoryPointDto(now, new BigDecimal("206000"))
                ));
        when(fxRateHistoryRepository.findHistoryPoints(eq("XAUTRY"), eq(now.minus(30, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of(
                        new HistoryPointDto(now.minus(30, ChronoUnit.DAYS), new BigDecimal("190000")),
                        new HistoryPointDto(now, new BigDecimal("206000"))
                ));
        when(fxRateHistoryRepository.findHistoryPoints(eq("XAUTRY"), any(Instant.class), eq(toExclusive)))
                .thenReturn(List.of(
                        new HistoryPointDto(now.minus(90, ChronoUnit.DAYS), new BigDecimal("180000")),
                        new HistoryPointDto(now, new BigDecimal("206000"))
                ));

        HistoricalMarketDataReadServiceImpl svc =
                new HistoricalMarketDataReadServiceImpl(
                        marketPriceHistoryRepository, fxRateHistoryRepository, fundNavHistoryRepository, null, clock);

        Map<String, MarketPriceSummaryDto> out = svc.getPriceSummary(List.of("XAUTRY"));
        MarketPriceSummaryDto dto = out.get("XAUTRY");
        assertNotNull(dto);
        assertEquals(0, new BigDecimal("206000").compareTo(dto.price()));
        org.junit.jupiter.api.Assertions.assertTrue(dto.change1D() > 0d);
        org.junit.jupiter.api.Assertions.assertTrue(dto.change1M() > 0d);
    }
}
