package com.company.analytics.processing.infrastructure.persistence;

import com.company.analytics.processing.domain.AnalyticsTrendMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** {@link AnalyticsTrendMetric} entity'si için Spring Data JPA repository. */
public interface AnalyticsTrendMetricRepository
        extends JpaRepository<AnalyticsTrendMetric, Long> {

    /** Instrument ve işlem tarihine göre trend metric kaydını döner. */
    Optional<AnalyticsTrendMetric> findByInstrumentIdAndTradeDate(
            Long instrumentId,
            LocalDate tradeDate
    );

    /** Sembole göre trend metric kayıtlarını işlem tarihine göre artan sırada döner. */
    List<AnalyticsTrendMetric> findByInstrumentSymbolOrderByTradeDateAsc(String instrumentSymbol);
}
