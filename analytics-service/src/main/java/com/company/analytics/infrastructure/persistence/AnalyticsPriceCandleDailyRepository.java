package com.company.analytics.infrastructure.persistence;

import com.company.analytics.domain.AnalyticsPriceCandleDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalyticsPriceCandleDailyRepository
        extends JpaRepository<AnalyticsPriceCandleDaily, Long> {

    Optional<AnalyticsPriceCandleDaily> findByInstrumentIdAndCandleDate(
            Long instrumentId,
            LocalDate candleDate
    );

    List<AnalyticsPriceCandleDaily> findByInstrumentSymbolOrderByCandleDateAsc(String instrumentSymbol);

    List<AnalyticsPriceCandleDaily> findByInstrumentSymbolAndCandleDateBetweenOrderByCandleDateAsc(
            String instrumentSymbol,
            LocalDate from,
            LocalDate to
    );
}