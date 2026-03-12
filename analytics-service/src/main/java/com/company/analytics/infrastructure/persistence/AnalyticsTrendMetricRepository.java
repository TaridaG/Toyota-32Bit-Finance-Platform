package com.company.analytics.infrastructure.persistence;

import com.company.analytics.domain.AnalyticsTrendMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalyticsTrendMetricRepository
        extends JpaRepository<AnalyticsTrendMetric, Long> {

    Optional<AnalyticsTrendMetric> findByInstrumentIdAndTradeDate(
            Long instrumentId,
            LocalDate tradeDate
    );

    List<AnalyticsTrendMetric> findByInstrumentSymbolOrderByTradeDateAsc(String instrumentSymbol);
}