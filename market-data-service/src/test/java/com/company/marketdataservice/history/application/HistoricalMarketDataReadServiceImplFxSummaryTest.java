package com.company.marketdataservice.history.application;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceSummaryDto;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.FxRateHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
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
class HistoricalMarketDataReadServiceImplFxSummaryTest {

    @Mock
    private MarketPriceHistoryRepository marketPriceHistoryRepository;

    @Mock
    private FxRateHistoryRepository fxRateHistoryRepository;

    @Mock
    private FundNavHistoryRepository fundNavHistoryRepository;

    @Test
    void fxSummary_readsFromFxRateHistory_notMarketPriceHistory() {
        Instant now = Instant.parse("2026-05-16T12:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        Instant toExclusive = now.plus(1, ChronoUnit.DAYS);

        when(fxRateHistoryRepository.findLatestHistoryPoint(eq("USDTRY"), any(Pageable.class)))
                .thenReturn(List.of(new HistoryPointDto(now, new BigDecimal("32.50"))));
        when(fxRateHistoryRepository.findHistoryPoints(eq("USDTRY"), eq(now.minus(1, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of(
                        new HistoryPointDto(now.minus(1, ChronoUnit.DAYS), new BigDecimal("32.00")),
                        new HistoryPointDto(now, new BigDecimal("32.50"))
                ));
        when(fxRateHistoryRepository.findHistoryPoints(eq("USDTRY"), eq(now.minus(7, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of(
                        new HistoryPointDto(now.minus(7, ChronoUnit.DAYS), new BigDecimal("32.10")),
                        new HistoryPointDto(now, new BigDecimal("32.50"))
                ));
        when(fxRateHistoryRepository.findHistoryPoints(eq("USDTRY"), eq(now.minus(30, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of(
                        new HistoryPointDto(now.minus(30, ChronoUnit.DAYS), new BigDecimal("31.00")),
                        new HistoryPointDto(now, new BigDecimal("32.50"))
                ));
        when(fxRateHistoryRepository.findHistoryPoints(eq("USDTRY"), eq(now.minus(90, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of());
        when(fxRateHistoryRepository.findHistoryPoints(eq("USDTRY"), eq(now.minus(180, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of());
        when(fxRateHistoryRepository.findHistoryPoints(eq("USDTRY"), eq(now.minus(365, ChronoUnit.DAYS)), eq(toExclusive)))
                .thenReturn(List.of());

        HistoricalMarketDataReadServiceImpl svc =
                new HistoricalMarketDataReadServiceImpl(
                        marketPriceHistoryRepository, fxRateHistoryRepository, fundNavHistoryRepository, null, null, clock);

        Map<String, MarketPriceSummaryDto> out = svc.getPriceSummary(List.of("USDTRY"));
        MarketPriceSummaryDto dto = out.get("USDTRY");
        assertNotNull(dto);
        assertEquals(0, new BigDecimal("32.50").compareTo(dto.price()));
        assertEquals(1.5625d, dto.change1D(), 1e-9);
        assertEquals(1.246106d, dto.change1W(), 1e-4);
        assertEquals(4.83871d, dto.change1M(), 1e-4);
    }
}
