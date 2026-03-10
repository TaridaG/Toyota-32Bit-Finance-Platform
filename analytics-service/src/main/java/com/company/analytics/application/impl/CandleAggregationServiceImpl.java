package com.company.analytics.application.impl;

import com.company.analytics.application.CandleAggregationService;
import com.company.analytics.domain.AnalyticsPriceCandleDaily;
import com.company.analytics.event.TransactionExecutedEvent;
import com.company.analytics.infrastructure.persistence.AnalyticsPriceCandleDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
@Transactional
public class CandleAggregationServiceImpl implements CandleAggregationService {

    private final AnalyticsPriceCandleDailyRepository repository;

    @Override
    public void process(TransactionExecutedEvent event) {
        LocalDate candleDate = event.getExecutedAt()
                .atZone(ZoneOffset.UTC)
                .toLocalDate();

        AnalyticsPriceCandleDaily candle = repository
                .findByInstrumentIdAndCandleDate(event.getInstrumentId(), candleDate)
                .orElseGet(() -> AnalyticsPriceCandleDaily.create(
                        event.getInstrumentId(),
                        event.getInstrumentSymbol(),
                        candleDate,
                        event.getPrice(),
                        event.getQuantity()
                ));

        if (candle.getId() != null) {
            candle.applyTrade(event.getPrice(), event.getQuantity());
        }

        repository.save(candle);
    }
}