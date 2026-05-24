package com.company.analytics.query.application;

import com.company.analytics.processing.infrastructure.persistence.AnalyticsMovingAverageRepository;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsPriceCandleDailyRepository;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsPriceCandleRepository;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsRSIRepository;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsTrendMetricRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AnalyticsQueryServiceImplTest {

    @Mock
    private AnalyticsPriceCandleDailyRepository candleRepository;
    @Mock
    private AnalyticsPriceCandleRepository multiIntervalCandleRepository;
    @Mock
    private AnalyticsMovingAverageRepository movingAverageRepository;
    @Mock
    private AnalyticsRSIRepository rsiRepository;
    @Mock
    private AnalyticsTrendMetricRepository trendMetricRepository;

    private AnalyticsQueryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AnalyticsQueryServiceImpl(
                candleRepository,
                multiIntervalCandleRepository,
                movingAverageRepository,
                rsiRepository,
                trendMetricRepository
        );
    }

    @Test
    void getCandles_rejectsBlankSymbol() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.getCandles("  ", null, null)
        );
        assertEquals("symbol is required", ex.getMessage());
    }

    @Test
    void getCandles_rejectsFromAfterTo() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.getCandles(
                        "BTCUSDT",
                        LocalDate.of(2026, 5, 10),
                        LocalDate.of(2026, 5, 1)
                )
        );
        assertEquals("'from' must not be after 'to'", ex.getMessage());
    }
}
