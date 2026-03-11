package com.company.analytics.application.impl;

import com.company.analytics.application.VWAPAggregationService;
import com.company.analytics.domain.AnalyticsVWAPDaily;
import com.company.analytics.event.TransactionExecutedEvent;
import com.company.analytics.infrastructure.persistence.AnalyticsVWAPRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class VWAPAggregationServiceImpl
        implements VWAPAggregationService {

    private final AnalyticsVWAPRepository repository;

    @Transactional
    @Override
    public void process(TransactionExecutedEvent event) {

        LocalDate tradeDate =
                event.getExecutedAt()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate();

        AnalyticsVWAPDaily vwap =
                repository.findByInstrumentIdAndTradeDate(
                        event.getInstrumentId(),
                        tradeDate
                ).orElseGet(() ->
                        AnalyticsVWAPDaily.create(
                                event.getInstrumentId(),
                                event.getInstrumentSymbol(),
                                tradeDate
                        )
                );

        vwap.applyTrade(
                event.getPrice(),
                event.getQuantity()
        );

        repository.save(vwap);

    }
}