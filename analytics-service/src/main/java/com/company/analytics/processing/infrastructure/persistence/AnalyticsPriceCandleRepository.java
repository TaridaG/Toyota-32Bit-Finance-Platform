package com.company.analytics.processing.infrastructure.persistence;

import com.company.analytics.processing.domain.AnalyticsPriceCandle;
import com.company.analytics.processing.domain.enums.CandleInterval;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** {@link AnalyticsPriceCandle} entity'si için Spring Data JPA repository. */
public interface AnalyticsPriceCandleRepository extends JpaRepository<AnalyticsPriceCandle, Long> {

    /** Instrument, candle interval ve açılış zamanına göre tek bir mum kaydını döner. */
    Optional<AnalyticsPriceCandle> findByInstrumentIdAndCandleIntervalAndOpenTime(
            Long instrumentId,
            CandleInterval candleInterval,
            Instant openTime
    );

    /** Sembol ve candle interval'e göre mumları açılış zamanına göre artan sırada döner. */
    List<AnalyticsPriceCandle> findByInstrumentSymbolAndCandleIntervalOrderByOpenTimeAsc(
            String symbol,
            CandleInterval candleInterval
    );

    /** Instrument ve candle interval'e göre mumları açılış zamanına göre artan sırada döner. */
    List<AnalyticsPriceCandle> findByInstrumentIdAndCandleIntervalOrderByOpenTimeAsc(
            Long instrumentId,
            CandleInterval candleInterval
    );

    /** Sembol, candle interval ve zaman aralığına göre mumları açılış zamanına göre artan sırada döner. */
    List<AnalyticsPriceCandle> findByInstrumentSymbolAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
            String symbol,
            CandleInterval candleInterval,
            Instant from,
            Instant to
    );

    /** Instrument, candle interval ve zaman aralığına göre mumları açılış zamanına göre artan sırada döner. */
    List<AnalyticsPriceCandle> findByInstrumentIdAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
            Long instrumentId,
            CandleInterval candleInterval,
            Instant from,
            Instant to
    );

    /** Belirtilen zamana kadar olan son 60 mumu açılış zamanına göre azalan sırada döner. */
    List<AnalyticsPriceCandle> findTop60ByInstrumentIdAndCandleIntervalAndOpenTimeLessThanEqualOrderByOpenTimeDesc(
            Long instrumentId,
            CandleInterval candleInterval,
            Instant to
    );

    /** Belirtilen zamana kadar olan son 2 mumu açılış zamanına göre azalan sırada döner. */
    List<AnalyticsPriceCandle> findTop2ByInstrumentIdAndCandleIntervalAndOpenTimeLessThanEqualOrderByOpenTimeDesc(
            Long instrumentId,
            CandleInterval candleInterval,
            Instant to
    );
}
