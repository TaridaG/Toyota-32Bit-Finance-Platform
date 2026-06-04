package com.company.marketdataservice.history.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.FxRateHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceSummaryDto;
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
class HistoricalMarketDataReadServiceImplJpySummaryTest {

    @Mock
    private MarketPriceHistoryRepository marketPriceHistoryRepository;

    @Mock
    private FxRateHistoryRepository fxRateHistoryRepository;

    @Mock
    private FundNavHistoryRepository fundNavHistoryRepository;

    @Test
    void jpyTrySummary_mixedTcmbAndEvdsScale_yieldsSaneOneYearChange() {
        Instant now = Instant.parse("2026-05-16T12:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        Instant toExclusive = now.plus(1, ChronoUnit.DAYS);

        when(fxRateHistoryRepository.findLatestHistoryPoint(eq("JPYTRY"), any(Pageable.class)))
                .thenReturn(List.of(new HistoryPointDto(now, new BigDecimal("28.70"))));
        when(fxRateHistoryRepository.findHistoryPoints(eq("JPYTRY"), eq(now.minus(7, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of(
                        new HistoryPointDto(now.minus(7, ChronoUnit.DAYS), new BigDecimal("0.285")),
                        new HistoryPointDto(now, new BigDecimal("28.70"))));
        when(fxRateHistoryRepository.findHistoryPoints(eq("JPYTRY"), eq(now.minus(30, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of(
                        new HistoryPointDto(now.minus(30, ChronoUnit.DAYS), new BigDecimal("0.280")),
                        new HistoryPointDto(now, new BigDecimal("28.70"))));
        when(fxRateHistoryRepository.findHistoryPoints(eq("JPYTRY"), eq(now.minus(90, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of());
        when(fxRateHistoryRepository.findHistoryPoints(eq("JPYTRY"), eq(now.minus(180, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of());
        when(fxRateHistoryRepository.findHistoryPoints(eq("JPYTRY"), eq(now.minus(365, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of(
                        new HistoryPointDto(now.minus(365, ChronoUnit.DAYS), new BigDecimal("0.270")),
                        new HistoryPointDto(now, new BigDecimal("28.70"))));
        when(fxRateHistoryRepository.findLastTwoDailyCloses(eq("JPYTRY")))
                .thenReturn(List.of(
                        new FxRateHistoryRepository.DailyCloseView() {
                            @Override
                            public java.time.LocalDate getDay() {
                                return java.time.LocalDate.of(2026, 5, 16);
                            }

                            @Override
                            public BigDecimal getPrice() {
                                return new BigDecimal("28.70");
                            }

                            @Override
                            public Instant getObservedAt() {
                                return now;
                            }
                        },
                        new FxRateHistoryRepository.DailyCloseView() {
                            @Override
                            public java.time.LocalDate getDay() {
                                return java.time.LocalDate.of(2026, 5, 15);
                            }

                            @Override
                            public BigDecimal getPrice() {
                                return new BigDecimal("0.285");
                            }

                            @Override
                            public Instant getObservedAt() {
                                return now.minus(1, ChronoUnit.DAYS);
                            }
                        }));

        HistoricalMarketDataReadServiceImpl svc =
                new HistoricalMarketDataReadServiceImpl(
                        marketPriceHistoryRepository, fxRateHistoryRepository, fundNavHistoryRepository, null, null, clock);

        Map<String, MarketPriceSummaryDto> out = svc.getPriceSummary(List.of("JPYTRY"));
        MarketPriceSummaryDto dto = out.get("JPYTRY");
        assertNotNull(dto);
        assertEquals(0, new BigDecimal("0.287").compareTo(dto.price()));
        assertEquals(0.701754d, dto.change1D(), 1e-3);
        assertEquals(2.5d, dto.change1M(), 1e-3);
        assertEquals(6.296296d, dto.change1Y(), 1e-3);
    }
}
