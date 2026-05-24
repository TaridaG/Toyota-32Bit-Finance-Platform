package com.company.analytics.processing.infrastructure.persistence;

import com.company.analytics.processing.domain.AnalyticsMovingAverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** {@link AnalyticsMovingAverage} entity'si için Spring Data JPA repository. */
public interface AnalyticsMovingAverageRepository
        extends JpaRepository<AnalyticsMovingAverage,Long> {

    /** Instrument ve işlem tarihine göre moving average kaydını döner. */
    Optional<AnalyticsMovingAverage> findByInstrumentIdAndTradeDate(
            Long instrumentId,
            LocalDate tradeDate
    );

    /** Sembole göre tüm moving average kayıtlarını döner. */
    List<AnalyticsMovingAverage> findByInstrumentSymbol(String instrumentSymbol);

}
