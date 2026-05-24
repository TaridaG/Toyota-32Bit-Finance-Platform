package com.company.analytics.processing.application;

import com.company.analytics.processing.domain.AnalyticsPriceCandle;
import com.company.analytics.processing.domain.AnalyticsPriceCandleDaily;
import com.company.analytics.processing.domain.enums.CandleInterval;
import com.company.analytics.processing.domain.event.AnalyticsMarketPriceEvent;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsPriceCandleDailyRepository;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsPriceCandleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandleAggregationServiceImplTest {

    @Mock
    private AnalyticsPriceCandleDailyRepository dailyRepository;

    @Mock
    private AnalyticsPriceCandleRepository repository;

    @InjectMocks
    private CandleAggregationServiceImpl service;

    @Test
    void process_createsDailyAndIntervalCandles() {
        Instant occurredAt = LocalDate.of(2026, 5, 24).atStartOfDay().toInstant(ZoneOffset.UTC);
        AnalyticsMarketPriceEvent event = new AnalyticsMarketPriceEvent(
                "evt-1",
                42L,
                "BTCUSDT",
                new BigDecimal("100"),
                occurredAt
        );

        when(dailyRepository.findByInstrumentIdAndCandleDate(42L, LocalDate.of(2026, 5, 24)))
                .thenReturn(Optional.empty());
        when(repository.findByInstrumentIdAndCandleIntervalAndOpenTime(
                eq(42L), any(CandleInterval.class), any(Instant.class)))
                .thenReturn(Optional.empty());

        service.process(event);

        verify(dailyRepository).save(any(AnalyticsPriceCandleDaily.class));
        verify(repository, atLeastOnce()).save(any(AnalyticsPriceCandle.class));
    }
}
