package com.company.analytics.infrastructure.persistence;

import com.company.analytics.domain.AnalyticsVWAPDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalyticsVWAPRepository
        extends JpaRepository<AnalyticsVWAPDaily,Long> {

    Optional<AnalyticsVWAPDaily> findByInstrumentIdAndTradeDate(
            Long instrumentId,
            LocalDate tradeDate
    );

    List<AnalyticsVWAPDaily> findByInstrumentSymbol(String instrumentSymbol);
}