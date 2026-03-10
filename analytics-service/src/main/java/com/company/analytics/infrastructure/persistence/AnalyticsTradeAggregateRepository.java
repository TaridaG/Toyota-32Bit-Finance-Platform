package com.company.analytics.infrastructure.persistence;

import com.company.analytics.domain.AnalyticsTradeAggregateDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalyticsTradeAggregateRepository
        extends JpaRepository<AnalyticsTradeAggregateDaily, Long> {

    Optional<AnalyticsTradeAggregateDaily>
    findByInstrumentIdAndTradeDate(Long instrumentId, LocalDate tradeDate);

    List<AnalyticsTradeAggregateDaily> findByInstrumentSymbol(String symbol);

}