package com.company.analytics.application.impl;

import com.company.analytics.application.TrendMetricService;
import com.company.analytics.domain.AnalyticsMovingAverage;
import com.company.analytics.domain.AnalyticsPriceCandleDaily;
import com.company.analytics.domain.AnalyticsTrendMetric;
import com.company.analytics.domain.enums.TrendDirection;
import com.company.analytics.event.AnalyticsMarketPriceEvent;
import com.company.analytics.infrastructure.persistence.AnalyticsMovingAverageRepository;
import com.company.analytics.infrastructure.persistence.AnalyticsPriceCandleDailyRepository;
import com.company.analytics.infrastructure.persistence.AnalyticsTrendMetricRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TrendMetricServiceImpl implements TrendMetricService {

    private static final int MOMENTUM_LOOKBACK_DAYS = 7;
    private static final int SLOPE_LOOKBACK_DAYS = 30;

    private final AnalyticsPriceCandleDailyRepository candleRepository;
    private final AnalyticsMovingAverageRepository movingAverageRepository;
    private final AnalyticsTrendMetricRepository trendMetricRepository;

    @Override
    public void process(AnalyticsMarketPriceEvent event) {
        LocalDate tradeDate = event.occurredAt()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        Optional<AnalyticsPriceCandleDaily> currentCandleOpt =
                candleRepository.findByInstrumentIdAndCandleDate(
                        event.instrumentId(),
                        tradeDate
                );

        if (currentCandleOpt.isEmpty()) {
            return;
        }

        AnalyticsPriceCandleDaily currentCandle = currentCandleOpt.get();

        TrendDirection trendDirection = resolveTrendDirection(
                event.instrumentId(),
                tradeDate
        );

        BigDecimal momentum = calculateMomentum(
                event.instrumentSymbol(),
                tradeDate,
                currentCandle.getClosePrice()
        );

        BigDecimal slope = calculateSlope(
                event.instrumentSymbol(),
                tradeDate,
                currentCandle.getClosePrice()
        );

        AnalyticsTrendMetric metric = trendMetricRepository
                .findByInstrumentIdAndTradeDate(event.instrumentId(), tradeDate)
                .orElseGet(() -> AnalyticsTrendMetric.create(
                        event.instrumentId(),
                        event.instrumentSymbol(),
                        tradeDate
                ));

        metric.update(trendDirection, momentum, slope);

        trendMetricRepository.save(metric);
    }

    private TrendDirection resolveTrendDirection(Long instrumentId, LocalDate tradeDate) {
        Optional<AnalyticsMovingAverage> movingAverageOpt =
                movingAverageRepository.findByInstrumentIdAndTradeDate(instrumentId, tradeDate);

        if (movingAverageOpt.isEmpty()) {
            return TrendDirection.NEUTRAL;
        }

        AnalyticsMovingAverage ma = movingAverageOpt.get();

        if (ma.getMa7() == null || ma.getMa30() == null) {
            return TrendDirection.NEUTRAL;
        }

        int comparison = ma.getMa7().compareTo(ma.getMa30());

        if (comparison > 0) {
            return TrendDirection.BULLISH;
        }

        if (comparison < 0) {
            return TrendDirection.BEARISH;
        }

        return TrendDirection.NEUTRAL;
    }

    private BigDecimal calculateMomentum(
            String symbol,
            LocalDate tradeDate,
            BigDecimal currentClose
    ) {
        List<AnalyticsPriceCandleDaily> candles =
                candleRepository.findByInstrumentSymbolOrderByCandleDateAsc(symbol);

        int currentIndex = indexOfDate(candles, tradeDate);
        if (currentIndex < 0) {
            return null;
        }

        int referenceIndex = currentIndex - MOMENTUM_LOOKBACK_DAYS;
        if (referenceIndex < 0) {
            return null;
        }

        BigDecimal referenceClose = candles.get(referenceIndex).getClosePrice();
        return currentClose.subtract(referenceClose);
    }

    private BigDecimal calculateSlope(
            String symbol,
            LocalDate tradeDate,
            BigDecimal currentClose
    ) {
        List<AnalyticsPriceCandleDaily> candles =
                candleRepository.findByInstrumentSymbolOrderByCandleDateAsc(symbol);

        int currentIndex = indexOfDate(candles, tradeDate);
        if (currentIndex < 0) {
            return null;
        }

        int referenceIndex = currentIndex - SLOPE_LOOKBACK_DAYS;
        if (referenceIndex < 0) {
            return null;
        }

        BigDecimal referenceClose = candles.get(referenceIndex).getClosePrice();

        return currentClose
                .subtract(referenceClose)
                .divide(
                        BigDecimal.valueOf(SLOPE_LOOKBACK_DAYS),
                        8,
                        RoundingMode.HALF_UP
                );
    }

    private int indexOfDate(List<AnalyticsPriceCandleDaily> candles, LocalDate tradeDate) {
        for (int i = 0; i < candles.size(); i++) {
            if (candles.get(i).getCandleDate().equals(tradeDate)) {
                return i;
            }
        }
        return -1;
    }
}