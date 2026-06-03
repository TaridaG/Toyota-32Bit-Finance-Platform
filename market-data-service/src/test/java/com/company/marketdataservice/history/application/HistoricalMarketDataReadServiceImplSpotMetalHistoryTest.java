package com.company.marketdataservice.history.application;

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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HistoricalMarketDataReadServiceImplSpotMetalHistoryTest {

    @Mock
    private MarketPriceHistoryRepository marketPriceHistoryRepository;
    @Mock
    private FxRateHistoryRepository fxRateHistoryRepository;
    @Mock
    private FundNavHistoryRepository fundNavHistoryRepository;

    private HistoricalMarketDataReadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HistoricalMarketDataReadServiceImpl(
                marketPriceHistoryRepository,
                fxRateHistoryRepository,
                fundNavHistoryRepository,
                null,
                null,
                null
        );
    }

    @Test
    void getFxHistory_usesProviderPriorityQueryForSpotMetals() {
        Instant t = Instant.parse("2026-05-01T00:00:00Z");
        when(fxRateHistoryRepository.findSpotMetalHistoryPoints(eq("XAUTRY"), any(), any()))
                .thenReturn(List.of(new HistoryPointDto(t, new BigDecimal("204000"))));

        service.getFxHistory("XAUTRY", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1));

        verify(fxRateHistoryRepository).findSpotMetalHistoryPoints(eq("XAUTRY"), any(), any());
    }

    @Test
    void getFxHistory_usesPlainQueryForFiatFx() {
        when(fxRateHistoryRepository.findHistoryPoints(eq("USDTRY"), any(), any()))
                .thenReturn(List.of());

        service.getFxHistory("USDTRY", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1));

        verify(fxRateHistoryRepository).findHistoryPoints(eq("USDTRY"), any(), any());
    }
}
