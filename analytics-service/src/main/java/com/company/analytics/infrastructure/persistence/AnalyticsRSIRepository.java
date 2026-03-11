package com.company.analytics.infrastructure.persistence;

import com.company.analytics.domain.AnalyticsRSI;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalyticsRSIRepository
        extends JpaRepository<AnalyticsRSI,Long> {

    Optional<AnalyticsRSI> findByInstrumentIdAndTradeDate(
            Long instrumentId,
            LocalDate tradeDate
    );
    List<AnalyticsRSI> findByInstrumentSymbol(String instrumentSymbol);

}