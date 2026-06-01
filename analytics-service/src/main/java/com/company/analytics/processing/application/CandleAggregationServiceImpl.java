package com.company.analytics.processing.application;

import com.company.analytics.processing.application.CandleAggregationService;
import com.company.analytics.processing.application.util.CandleTradePriceUtil;
import com.company.analytics.processing.application.util.TimeBucketUtil;
import com.company.analytics.processing.domain.AnalyticsPriceCandle;
import com.company.analytics.processing.domain.AnalyticsPriceCandleDaily;
import com.company.analytics.processing.domain.enums.CandleInterval;
import com.company.analytics.processing.domain.event.AnalyticsMarketPriceEvent;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsPriceCandleDailyRepository;
import com.company.analytics.processing.infrastructure.persistence.AnalyticsPriceCandleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.math.BigDecimal;

/** Market price event'lerini günlük ve çoklu interval candle kayıtlarına agregasyon yapan servis. */
@Service
@RequiredArgsConstructor
@Transactional
public class CandleAggregationServiceImpl implements CandleAggregationService {

    private final AnalyticsPriceCandleDailyRepository dailyRepository;
    private final AnalyticsPriceCandleRepository repository;

    /** Event fiyatını günlük ve tüm desteklenen interval candle bucket'larına uygular. */
    @Override
    public void process(AnalyticsMarketPriceEvent event) {
        if (!CandleTradePriceUtil.isValidTradePrice(event.price())) {
            return;
        }
        processDaily(event);
        processForInterval(event, CandleInterval.ONE_MINUTE);
        processForInterval(event, CandleInterval.FIVE_MINUTES);
        processForInterval(event, CandleInterval.ONE_HOUR);
        processForInterval(event, CandleInterval.ONE_DAY);
    }

    private void processDaily(AnalyticsMarketPriceEvent event) {
        LocalDate candleDate = event.occurredAt()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();
        AnalyticsPriceCandleDaily candle = dailyRepository
                .findByInstrumentIdAndCandleDate(event.instrumentId(), candleDate)
                .orElseGet(() -> AnalyticsPriceCandleDaily.create(
                        event.instrumentId(),
                        event.instrumentSymbol(),
                        candleDate,
                        event.price(),
                        BigDecimal.ONE
                ));
        if (candle.getId() != null) {
            candle.applyTrade(event.price(), BigDecimal.ONE);
        }
        dailyRepository.save(candle);
    }

    private void processForInterval(AnalyticsMarketPriceEvent event, CandleInterval interval) {
        Instant openTime = TimeBucketUtil.truncate(event.occurredAt(), interval);
        AnalyticsPriceCandle candle = repository
                .findByInstrumentIdAndCandleIntervalAndOpenTime(
                        event.instrumentId(),
                        interval,
                        openTime
                )
                .orElseGet(() -> AnalyticsPriceCandle.create(
                        event.instrumentId(),
                        event.instrumentSymbol(),
                        interval,
                        openTime,
                        event.price(),
                        BigDecimal.ONE
                ));
        if (candle.getId() != null) {
            candle.applyTrade(event.price(), BigDecimal.ONE);
        }
        repository.save(candle);
    }
}