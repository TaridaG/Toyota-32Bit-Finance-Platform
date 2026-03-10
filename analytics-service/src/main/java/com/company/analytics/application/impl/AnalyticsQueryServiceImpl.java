package com.company.analytics.application.impl;

import com.company.analytics.application.AnalyticsQueryService;
import com.company.analytics.dto.AnalyticsSummaryResponse;
import com.company.analytics.dto.CandleResponse;
import com.company.analytics.infrastructure.persistence.AnalyticsPriceCandleDailyRepository;
import com.company.analytics.infrastructure.persistence.AnalyticsTradeAggregateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final AnalyticsTradeAggregateRepository aggregateRepository;
    private final AnalyticsPriceCandleDailyRepository candleRepository;

    @Override
    public List<AnalyticsSummaryResponse> getDaily(String symbol) {
        return aggregateRepository.findByInstrumentSymbol(symbol)
                .stream()
                .map(AnalyticsSummaryResponse::from)
                .toList();
    }

    @Override
    public List<CandleResponse> getCandles(String symbol, LocalDate from, LocalDate to) {
        return candleRepository
                .findByInstrumentSymbolAndCandleDateBetweenOrderByCandleDateAsc(symbol, from, to)
                .stream()
                .map(CandleResponse::from)
                .toList();
    }
}