package com.company.analytics.processing.infrastructure.persistence;

import com.company.analytics.processing.domain.AnalyticsPriceCandleDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** {@link AnalyticsPriceCandleDaily} entity'si için Spring Data JPA repository. */
public interface AnalyticsPriceCandleDailyRepository
        extends JpaRepository<AnalyticsPriceCandleDaily, Long> {

    /** Instrument ve mum tarihine göre günlük candle kaydını döner. */
    Optional<AnalyticsPriceCandleDaily> findByInstrumentIdAndCandleDate(
            Long instrumentId,
            LocalDate candleDate
    );

    /** Instrument kimliğine göre günlük mumları tarihe göre artan sırada döner. */
    List<AnalyticsPriceCandleDaily> findByInstrumentIdOrderByCandleDateAsc(Long instrumentId);

    /** Sembole göre günlük mumları tarihe göre artan sırada döner. */
    List<AnalyticsPriceCandleDaily> findByInstrumentSymbolOrderByCandleDateAsc(String instrumentSymbol);

    /** Sembol ve tarih aralığına göre günlük mumları tarihe göre artan sırada döner. */
    List<AnalyticsPriceCandleDaily> findByInstrumentSymbolAndCandleDateBetweenOrderByCandleDateAsc(
            String instrumentSymbol,
            LocalDate from,
            LocalDate to
    );

}
