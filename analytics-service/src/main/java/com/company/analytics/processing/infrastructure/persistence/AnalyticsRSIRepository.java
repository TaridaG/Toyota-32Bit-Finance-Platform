package com.company.analytics.processing.infrastructure.persistence;

import com.company.analytics.processing.domain.AnalyticsRSI;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** {@link AnalyticsRSI} entity'si için Spring Data JPA repository. */
public interface AnalyticsRSIRepository
        extends JpaRepository<AnalyticsRSI,Long> {

    /** Instrument ve işlem tarihine göre RSI kaydını döner. */
    Optional<AnalyticsRSI> findByInstrumentIdAndTradeDate(
            Long instrumentId,
            LocalDate tradeDate
    );

    /** Sembole göre RSI kayıtlarını işlem tarihine göre artan sırada döner. */
    List<AnalyticsRSI> findByInstrumentSymbolOrderByTradeDateAsc(String instrumentSymbol);


}
