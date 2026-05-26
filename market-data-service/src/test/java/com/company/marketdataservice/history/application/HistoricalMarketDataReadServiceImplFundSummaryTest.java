package com.company.marketdataservice.history.application;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceSummaryDto;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryEntry;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.FxRateHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistoricalMarketDataReadServiceImplFundSummaryTest {

    @Mock
    private MarketPriceHistoryRepository marketPriceHistoryRepository;

    @Mock
    private FxRateHistoryRepository fxRateHistoryRepository;

    @Mock
    private FundNavHistoryRepository fundNavHistoryRepository;

    @Test
    void fundSummary_usesTrailingNavPoints_notRolling24hWindow() {
        Instant now = Instant.parse("2026-05-13T20:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        FundNavHistoryEntry yesterday = entry(1L, new BigDecimal("1.40"), Instant.parse("2026-05-12T07:00:00Z"));
        FundNavHistoryEntry today = entry(2L, new BigDecimal("1.50"), Instant.parse("2026-05-13T07:00:00Z"));

        when(fundNavHistoryRepository.findTopByFundCodeOrderByObservedAtDesc("TP2")).thenReturn(Optional.of(today));
        when(fundNavHistoryRepository.findTopByFundCodeAndObservedAtLessThanEqualOrderByObservedAtDesc("TP2", now))
                .thenReturn(Optional.of(today));
        when(fundNavHistoryRepository.findTopByFundCodeAndObservedAtLessThanEqualOrderByObservedAtDesc(
                        "TP2", now.minus(1, ChronoUnit.DAYS)))
                .thenReturn(Optional.of(yesterday));

        HistoricalMarketDataReadServiceImpl svc =
                new HistoricalMarketDataReadServiceImpl(
                        marketPriceHistoryRepository, fxRateHistoryRepository, fundNavHistoryRepository, null, null, clock);

        Map<String, MarketPriceSummaryDto> out = svc.getPriceSummary(List.of("FUND_TP2"));
        MarketPriceSummaryDto dto = out.get("FUND_TP2");
        assertTrue(dto != null);
        assertEquals(0, new BigDecimal("1.50").compareTo(dto.price()));
        double expected1d =
                new BigDecimal("1.50")
                        .subtract(new BigDecimal("1.40"))
                        .divide(new BigDecimal("1.40"), 8, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .doubleValue();
        assertEquals(expected1d, dto.change1D(), 1e-9);
    }

    private static FundNavHistoryEntry entry(long id, BigDecimal nav, Instant observedAt) {
        FundNavHistoryEntry e = new FundNavHistoryEntry();
        e.setId(id);
        e.setFundCode("TP2");
        e.setNav(nav);
        e.setObservedAt(observedAt);
        e.setProvider("TEFAS");
        e.setIngestTime(observedAt);
        return e;
    }
}
