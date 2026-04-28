package com.company.analytics.application;

import com.company.analytics.domain.AnalyticsPriceCandle;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureExtractionServiceTest {

    private final FeatureExtractionService service = new FeatureExtractionService(
            null,
            new SimpleMeterRegistry(),
            null
    );

    @Test
    void volatility_calculation_should_be_correct() {
        List<AnalyticsPriceCandle> candlesDesc = List.of(
                candle("120"),
                candle("100")
        );

        BigDecimal volatility = service.calculateVolatilityFromHourCandles(candlesDesc);

        assertTrue(volatility.subtract(new BigDecimal("10")).abs()
                .compareTo(new BigDecimal("0.000001")) <= 0);
    }

    @Test
    void momentum_calculation_should_be_correct() {
        List<AnalyticsPriceCandle> candlesDesc = List.of(
                candle("120"),
                candle("100")
        );

        BigDecimal momentum = service.calculateMomentumFromHourCandles(candlesDesc);

        assertEquals(new BigDecimal("0.2"), momentum.stripTrailingZeros());
    }

    private static AnalyticsPriceCandle candle(String closePrice) {
        AnalyticsPriceCandle candle = new AnalyticsPriceCandle();
        candle.setClosePrice(new BigDecimal(closePrice));
        return candle;
    }
}
