package com.company.marketdataservice.history.application;
import com.company.marketdataservice.history.application.HistoricalMarketDataReadServiceImpl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.FxRateHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistoricalMarketDataReadServiceImplMetalFuturesHistoryTest {

    @Mock
    private MarketPriceHistoryRepository marketPriceHistoryRepository;

    @Mock
    private FxRateHistoryRepository fxRateHistoryRepository;

    @Mock
    private FundNavHistoryRepository fundNavHistoryRepository;

    @Test
    void getPriceHistory_acceptsFiveYearRangeForGcFuturesAndUsesMarketCloses() {
        LocalDate to = LocalDate.of(2026, 5, 21);
        LocalDate from = to.minusYears(5);
        HistoryPointDto point = new HistoryPointDto(Instant.parse("2026-05-20T00:00:00Z"), new BigDecimal("4530"));
        when(marketPriceHistoryRepository.findHistoryPointsByPriceType(
                        eq("GC=F"), eq("MARKET"), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(point));

        HistoricalMarketDataReadServiceImpl service =
                new HistoricalMarketDataReadServiceImpl(
                        marketPriceHistoryRepository, fxRateHistoryRepository, fundNavHistoryRepository, null, null);

        List<HistoryPointDto> out = service.getPriceHistory("GC=F", from, to);

        assertEquals(1, out.size());
        verify(marketPriceHistoryRepository)
                .findHistoryPointsByPriceType(eq("GC=F"), eq("MARKET"), any(Instant.class), any(Instant.class));
    }
}
