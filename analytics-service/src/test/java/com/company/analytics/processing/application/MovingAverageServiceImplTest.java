package com.company.analytics.processing.application;

import com.company.analytics.processing.domain.AnalyticsMovingAverage;
import com.company.analytics.processing.domain.AnalyticsPriceCandleDaily;
import com.company.analytics.processing.domain.event.AnalyticsMarketPriceEvent;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsMovingAverageRepository;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsPriceCandleDailyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovingAverageServiceImplTest {

    @Mock
    private AnalyticsPriceCandleDailyRepository candleRepository;

    @Mock
    private AnalyticsMovingAverageRepository repository;

    @InjectMocks
    private MovingAverageServiceImpl service;

    @Test
    void process_persistsMovingAveragesWhenEnoughCandles() {
        LocalDate tradeDate = LocalDate.of(2026, 5, 24);
        Instant occurredAt = tradeDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        List<AnalyticsPriceCandleDaily> candles = new ArrayList<>();
        for (int i = 0; i < 90; i++) {
            candles.add(AnalyticsPriceCandleDaily.create(
                    9L,
                    "ETHUSDT",
                    tradeDate.minusDays(89 - i),
                    BigDecimal.valueOf(100 + i),
                    BigDecimal.ONE
            ));
        }

        when(candleRepository.findByInstrumentIdOrderByCandleDateAsc(9L)).thenReturn(candles);
        when(repository.findByInstrumentIdAndTradeDate(9L, tradeDate)).thenReturn(Optional.empty());

        service.process(new AnalyticsMarketPriceEvent(
                "evt-ma", 9L, "ETHUSDT", new BigDecimal("189"), occurredAt));

        ArgumentCaptor<AnalyticsMovingAverage> captor = ArgumentCaptor.forClass(AnalyticsMovingAverage.class);
        verify(repository).save(captor.capture());
        assertEquals(new BigDecimal("186.00000000"), captor.getValue().getMa7());
        assertEquals(new BigDecimal("174.50000000"), captor.getValue().getMa30());
        assertEquals(new BigDecimal("144.50000000"), captor.getValue().getMa90());
    }
}
