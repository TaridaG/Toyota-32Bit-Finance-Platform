package com.company.analytics.infrastructure.persistence;

import com.company.analytics.domain.AnalyticsPriceCandle;
import com.company.analytics.domain.enums.CandleInterval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AnalyticsPriceCandleRepository extends JpaRepository<AnalyticsPriceCandle, Long> {

    Optional<AnalyticsPriceCandle> findByInstrumentIdAndCandleIntervalAndOpenTime(
            Long instrumentId,
            CandleInterval candleInterval,
            Instant openTime
    );

    List<AnalyticsPriceCandle> findByInstrumentSymbolAndCandleIntervalOrderByOpenTimeAsc(
            String symbol,
            CandleInterval candleInterval
    );

    List<AnalyticsPriceCandle> findByInstrumentSymbolAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
            String symbol,
            CandleInterval candleInterval,
            Instant from,
            Instant to
    );
}