package com.company.analytics.application;

import com.company.analytics.domain.AnalyticsPriceCandle;
import com.company.analytics.domain.enums.CandleInterval;
import com.company.analytics.event.AnalyticsInsightEvent;
import com.company.analytics.event.AnalyticsMarketPriceEvent;
import com.company.analytics.feature.AnalyticsFeatureSnapshot;
import com.company.analytics.infrastructure.persistence.AnalyticsPriceCandleRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureExtractionService {

    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);
    private static final int VOLATILITY_WINDOW = 20;
    private static final int MOMENTUM_WINDOW = 5;
    private static final String SIMPLE_INSIGHT_TOPIC = "analytics.insight.simple";

    private final AnalyticsPriceCandleRepository candleRepository;
    private final MeterRegistry meterRegistry;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${analytics.features.enabled:true}")
    private boolean featuresEnabled;
    @Value("${notification.insight.threshold:0.08}")
    private BigDecimal simpleInsightThreshold;

    public Optional<AnalyticsFeatureSnapshot> extract(AnalyticsMarketPriceEvent event, String normalizedPriceType) {
        if (!featuresEnabled) {
            log.info("analytics_feature_extraction_skipped service=analytics-service instrumentId={} eventId={} reason=disabled",
                    event.instrumentId(), event.eventId());
            return Optional.empty();
        }

        List<AnalyticsPriceCandle> hourCandlesDesc = candleRepository
                .findTop60ByInstrumentIdAndCandleIntervalAndOpenTimeLessThanEqualOrderByOpenTimeDesc(
                        event.instrumentId(),
                        CandleInterval.ONE_HOUR,
                        event.occurredAt()
                );
        List<AnalyticsPriceCandle> dayCandlesDesc = candleRepository
                .findTop2ByInstrumentIdAndCandleIntervalAndOpenTimeLessThanEqualOrderByOpenTimeDesc(
                        event.instrumentId(),
                        CandleInterval.ONE_DAY,
                        event.occurredAt()
                );

        BigDecimal volatility = calculateVolatilityFromHourCandles(hourCandlesDesc);
        BigDecimal momentum = calculateMomentumFromHourCandles(hourCandlesDesc);
        BigDecimal priceChange1h = calculatePriceChange(hourCandlesDesc);
        BigDecimal priceChange24h = calculatePriceChange(dayCandlesDesc);

        AnalyticsFeatureSnapshot snapshot = new AnalyticsFeatureSnapshot(
                event.instrumentId(),
                event.occurredAt(),
                event.price(),
                volatility,
                momentum,
                priceChange1h,
                priceChange24h,
                normalizedPriceType
        );

        meterRegistry.counter(
                "analytics_feature_generated_total",
                "service", "analytics-service",
                "feature", "volatility",
                "priceType", normalizedPriceType
        ).increment();
        meterRegistry.counter(
                "analytics_feature_generated_total",
                "service", "analytics-service",
                "feature", "momentum",
                "priceType", normalizedPriceType
        ).increment();
        meterRegistry.counter(
                "analytics_feature_generated_total",
                "service", "analytics-service",
                "feature", "price_change",
                "priceType", normalizedPriceType
        ).increment();
        publishSimpleInsightIfNeeded(snapshot, event.instrumentSymbol());

        log.info("analytics_feature_snapshot_generated service=analytics-service instrumentId={} source={} ts={} price={} volatility={} momentum={} change1h={} change24h={}",
                snapshot.instrumentId(), snapshot.source(), snapshot.timestamp(), snapshot.price(),
                snapshot.volatility(), snapshot.momentum(), snapshot.priceChange1h(), snapshot.priceChange24h());
        return Optional.of(snapshot);
    }

    BigDecimal calculateVolatilityFromHourCandles(List<AnalyticsPriceCandle> hourCandlesDesc) {
        if (hourCandlesDesc == null || hourCandlesDesc.size() < 2) {
            return BigDecimal.ZERO;
        }
        List<AnalyticsPriceCandle> candles = sortedAsc(hourCandlesDesc);
        int windowStart = Math.max(0, candles.size() - VOLATILITY_WINDOW);
        List<AnalyticsPriceCandle> window = candles.subList(windowStart, candles.size());
        double mean = window.stream()
                .map(c -> c.getClosePrice().doubleValue())
                .reduce(0.0, Double::sum) / window.size();
        double variance = window.stream()
                .map(c -> Math.pow(c.getClosePrice().doubleValue() - mean, 2))
                .reduce(0.0, Double::sum) / window.size();
        return BigDecimal.valueOf(Math.sqrt(variance)).round(MC);
    }

    BigDecimal calculateMomentumFromHourCandles(List<AnalyticsPriceCandle> hourCandlesDesc) {
        if (hourCandlesDesc == null || hourCandlesDesc.size() < 2) {
            return BigDecimal.ZERO;
        }
        List<AnalyticsPriceCandle> candles = sortedAsc(hourCandlesDesc);
        int anchorIndex = Math.max(0, candles.size() - 1 - MOMENTUM_WINDOW);
        BigDecimal anchor = candles.get(anchorIndex).getClosePrice();
        BigDecimal current = candles.get(candles.size() - 1).getClosePrice();
        if (anchor == null || anchor.compareTo(BigDecimal.ZERO) == 0 || current == null) {
            return BigDecimal.ZERO;
        }
        return current.subtract(anchor).divide(anchor, MC);
    }

    private BigDecimal calculatePriceChange(List<AnalyticsPriceCandle> candlesDesc) {
        if (candlesDesc == null || candlesDesc.size() < 2) {
            return BigDecimal.ZERO;
        }
        List<AnalyticsPriceCandle> candles = sortedAsc(candlesDesc);
        BigDecimal previous = candles.get(candles.size() - 2).getClosePrice();
        BigDecimal current = candles.get(candles.size() - 1).getClosePrice();
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0 || current == null) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous).divide(previous, MC);
    }

    private List<AnalyticsPriceCandle> sortedAsc(List<AnalyticsPriceCandle> candlesDesc) {
        List<AnalyticsPriceCandle> candles = new java.util.ArrayList<>(candlesDesc);
        Collections.reverse(candles);
        return candles;
    }

    private void publishSimpleInsightIfNeeded(AnalyticsFeatureSnapshot snapshot, String symbol) {
        BigDecimal threshold = simpleInsightThreshold == null ? new BigDecimal("0.08") : simpleInsightThreshold;
        BigDecimal change = snapshot.priceChange1h() == null ? BigDecimal.ZERO : snapshot.priceChange1h();
        String direction = null;
        if (change.compareTo(threshold) >= 0) {
            direction = "UP";
        } else if (change.compareTo(threshold.negate()) <= 0) {
            direction = "DOWN";
        }
        if (direction == null) {
            return;
        }
        AnalyticsInsightEvent insightEvent = new AnalyticsInsightEvent(
                UUID.randomUUID(),
                snapshot.instrumentId(),
                symbol,
                change,
                direction,
                snapshot.timestamp()
        );
        kafkaTemplate.send(SIMPLE_INSIGHT_TOPIC, String.valueOf(snapshot.instrumentId()), insightEvent);
        meterRegistry.counter(
                "analytics_insight_triggered_total",
                "service", "analytics-service",
                "direction", direction
        ).increment();
    }
}
