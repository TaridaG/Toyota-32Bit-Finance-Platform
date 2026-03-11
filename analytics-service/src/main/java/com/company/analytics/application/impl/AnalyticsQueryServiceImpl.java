package com.company.analytics.application.impl;

import com.company.analytics.application.AnalyticsQueryService;
import com.company.analytics.dto.*;
import com.company.analytics.infrastructure.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final AnalyticsTradeAggregateRepository aggregateRepository;
    private final AnalyticsPriceCandleDailyRepository candleRepository;
    private final AnalyticsVWAPRepository vwapRepository;
    private final AnalyticsMovingAverageRepository movingAverageRepository;
    private final AnalyticsRSIRepository rsiRepository;

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
    @Override
    public List<VWAPResponse> getVWAP(String symbol) {
        return vwapRepository
                .findByInstrumentSymbol(symbol)
                .stream()
                .map(v -> VWAPResponse.builder()
                        .symbol(v.getInstrumentSymbol())
                        .date(v.getTradeDate())
                        .vwap(v.getVwap())
                        .build()
                )
                .toList();
    }
    @Override
    public List<MovingAverageResponse> getMovingAverage(String symbol) {
        return movingAverageRepository
                .findByInstrumentSymbol(symbol)
                .stream()
                .map(m ->
                        MovingAverageResponse.builder()
                                .symbol(m.getInstrumentSymbol())
                                .date(m.getTradeDate())
                                .ma7(m.getMa7())
                                .ma30(m.getMa30())
                                .ma90(m.getMa90())
                                .build()
                )
                .toList();
    }
    @Override
    public List<RSIResponse> getRSI(String symbol) {
        return rsiRepository
                .findByInstrumentSymbol(symbol)
                .stream()
                .map(r ->
                        RSIResponse.builder()
                                .symbol(r.getInstrumentSymbol())
                                .date(r.getTradeDate())
                                .rsi14(r.getRsi14())
                                .build()
                )
                .toList();
    }
}