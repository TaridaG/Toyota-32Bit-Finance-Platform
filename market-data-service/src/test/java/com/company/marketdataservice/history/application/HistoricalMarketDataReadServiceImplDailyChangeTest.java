package com.company.marketdataservice.history.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.FxRateHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceSummaryDto;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
class HistoricalMarketDataReadServiceImplDailyChangeTest {

    @Mock
    private MarketPriceHistoryRepository marketPriceHistoryRepository;

    @Mock
    private FxRateHistoryRepository fxRateHistoryRepository;

    @Mock
    private FundNavHistoryRepository fundNavHistoryRepository;

    @Test
    void stockSummary_usesLastTwoDailyCloses_notRolling24hWindow() {
        Instant now = Instant.parse("2026-05-24T15:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        when(marketPriceHistoryRepository.findLatestHistoryPoint(eq("GARAN"), any(Pageable.class)))
                .thenReturn(List.of(new HistoryPointDto(now, new BigDecimal("100.00"))));
        when(marketPriceHistoryRepository.findLastTwoDailyCloses("GARAN"))
                .thenReturn(List.of(
                        dailyClose(LocalDate.of(2026, 5, 23), new BigDecimal("100.00"), now),
                        dailyClose(LocalDate.of(2026, 5, 22), new BigDecimal("95.00"), now.minus(1, ChronoUnit.DAYS))));
        stubEmptyPeriodHistory("GARAN", now);

        HistoricalMarketDataReadServiceImpl svc =
                new HistoricalMarketDataReadServiceImpl(
                        marketPriceHistoryRepository, fxRateHistoryRepository, fundNavHistoryRepository, null, null, clock);

        Map<String, MarketPriceSummaryDto> out = svc.getPriceSummary(List.of("GARAN"));
        MarketPriceSummaryDto dto = out.get("GARAN");
        assertNotNull(dto);
        assertEquals(5.26315789d, dto.change1D(), 1e-6);
    }

    @Test
    void priceSummary_reusesWarmCache_forRepeatedReads() {
        Instant now = Instant.parse("2026-05-24T15:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        when(marketPriceHistoryRepository.findLatestHistoryPoint(eq("GARAN"), any(Pageable.class)))
                .thenReturn(List.of(new HistoryPointDto(now, new BigDecimal("100.00"))));
        when(marketPriceHistoryRepository.findLastTwoDailyCloses("GARAN"))
                .thenReturn(List.of(
                        dailyClose(LocalDate.of(2026, 5, 23), new BigDecimal("100.00"), now),
                        dailyClose(LocalDate.of(2026, 5, 22), new BigDecimal("95.00"), now.minus(1, ChronoUnit.DAYS))));
        stubEmptyPeriodHistory("GARAN", now);

        HistoricalMarketDataReadServiceImpl svc =
                new HistoricalMarketDataReadServiceImpl(
                        marketPriceHistoryRepository, fxRateHistoryRepository, fundNavHistoryRepository, null, null, clock);

        svc.getPriceSummary(List.of("GARAN"));
        svc.getPriceSummary(List.of("GARAN"));

        verify(marketPriceHistoryRepository, times(1)).findLatestHistoryPoint(eq("GARAN"), any(Pageable.class));
        verify(marketPriceHistoryRepository, times(1)).findLastTwoDailyCloses("GARAN");
    }

    private void stubEmptyPeriodHistory(String symbol, Instant now) {
        Instant toExclusive = now.plus(1, ChronoUnit.DAYS);
        when(marketPriceHistoryRepository.findHistoryPoints(eq(symbol), any(), eq(toExclusive)))
                .thenReturn(List.of());
    }

    private static MarketPriceHistoryRepository.DailyCloseView dailyClose(
            LocalDate day, BigDecimal price, Instant observedAt) {
        return new MarketPriceHistoryRepository.DailyCloseView() {
            @Override
            public LocalDate getDay() {
                return day;
            }

            @Override
            public BigDecimal getPrice() {
                return price;
            }

            @Override
            public Instant getObservedAt() {
                return observedAt;
            }
        };
    }
}
