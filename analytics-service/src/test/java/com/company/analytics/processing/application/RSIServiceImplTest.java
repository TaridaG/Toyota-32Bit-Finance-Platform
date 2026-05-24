package com.company.analytics.processing.application;

import com.company.analytics.processing.domain.AnalyticsPriceCandleDaily;
import com.company.analytics.processing.domain.AnalyticsRSI;
import com.company.analytics.processing.domain.event.AnalyticsMarketPriceEvent;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsPriceCandleDailyRepository;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsRSIRepository;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RSIServiceImplTest {

    @Mock
    private AnalyticsPriceCandleDailyRepository candleRepository;

    @Mock
    private AnalyticsRSIRepository repository;

    @InjectMocks
    private RSIServiceImpl service;

    @Test
    void process_persistsRsiWhenEnoughCandles() {
        LocalDate tradeDate = LocalDate.of(2026, 5, 24);
        Instant occurredAt = tradeDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        List<AnalyticsPriceCandleDaily> candles = new ArrayList<>();
        BigDecimal[] closes = {
                new BigDecimal("100"), new BigDecimal("102"), new BigDecimal("101"),
                new BigDecimal("103"), new BigDecimal("102"), new BigDecimal("104"),
                new BigDecimal("103"), new BigDecimal("105"), new BigDecimal("104"),
                new BigDecimal("106"), new BigDecimal("105"), new BigDecimal("107"),
                new BigDecimal("106"), new BigDecimal("108"), new BigDecimal("107"),
                new BigDecimal("109")
        };
        for (int i = 0; i < closes.length; i++) {
            candles.add(AnalyticsPriceCandleDaily.create(
                    7L, "BTCUSDT", tradeDate.minusDays(15 - i), closes[i], BigDecimal.ONE));
        }

        when(candleRepository.findByInstrumentIdOrderByCandleDateAsc(7L)).thenReturn(candles);
        when(repository.findByInstrumentIdAndTradeDate(7L, tradeDate)).thenReturn(Optional.empty());

        service.process(new AnalyticsMarketPriceEvent(
                "evt-rsi", 7L, "BTCUSDT", closes[closes.length - 1], occurredAt));

        ArgumentCaptor<AnalyticsRSI> captor = ArgumentCaptor.forClass(AnalyticsRSI.class);
        verify(repository).save(captor.capture());
        assertNotNull(captor.getValue().getRsi14());
    }
}
