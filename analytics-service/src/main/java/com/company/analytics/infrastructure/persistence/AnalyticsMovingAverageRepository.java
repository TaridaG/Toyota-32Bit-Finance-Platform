package com.company.analytics.infrastructure.persistence;

import com.company.analytics.domain.AnalyticsMovingAverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalyticsMovingAverageRepository
        extends JpaRepository<AnalyticsMovingAverage,Long> {

    Optional<AnalyticsMovingAverage> findByInstrumentIdAndTradeDate(
            Long instrumentId,
            LocalDate tradeDate
    );
    List<AnalyticsMovingAverage> findByInstrumentSymbol(String instrumentSymbol);

}