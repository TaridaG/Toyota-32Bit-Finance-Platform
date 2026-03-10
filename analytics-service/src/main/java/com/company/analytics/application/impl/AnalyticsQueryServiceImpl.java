package com.company.analytics.application.impl;

import com.company.analytics.application.AnalyticsQueryService;
import com.company.analytics.domain.AnalyticsTradeAggregateDaily;
import com.company.analytics.dto.AnalyticsSummaryResponse;
import com.company.analytics.infrastructure.persistence.AnalyticsTradeAggregateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final AnalyticsTradeAggregateRepository repository;

    @Override
    public List<AnalyticsSummaryResponse> getDaily(String symbol) {
        return repository.findByInstrumentSymbol(symbol)
                .stream()
                .map(AnalyticsSummaryResponse::from)
                .toList();
    }
}