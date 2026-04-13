package com.company.analytics.application.impl;

import com.company.analytics.application.AnalyticsQueryService;
import com.company.analytics.domain.enums.CandleInterval;
import com.company.analytics.dto.*;
import com.company.analytics.infrastructure.persistence.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnalyticsQueryServiceImpl implements AnalyticsQueryService {

    private final AnalyticsPriceCandleDailyRepository candleRepository;
    private final AnalyticsPriceCandleRepository multiIntervalCandleRepository;
    private final AnalyticsMovingAverageRepository movingAverageRepository;
    private final AnalyticsRSIRepository rsiRepository;
    private final AnalyticsTrendMetricRepository trendMetricRepository;

    @Override
    public List<CandleResponse> getCandles(String symbol, LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            return candleRepository
                    .findByInstrumentSymbolOrderByCandleDateAsc(symbol)
                    .stream()
                    .map(CandleResponse::from)
                    .toList();
        }
        return candleRepository
                .findByInstrumentSymbolAndCandleDateBetweenOrderByCandleDateAsc(symbol, from, to)
                .stream()
                .map(CandleResponse::from)
                .toList();
    }

    @Override
    public List<CandleResponse> getCandlesByInterval(
            String symbol,
            CandleInterval interval,
            LocalDate from,
            LocalDate to
    ) {
        if (from == null || to == null) {
            return multiIntervalCandleRepository
                    .findByInstrumentSymbolAndCandleIntervalOrderByOpenTimeAsc(symbol, interval)
                    .stream()
                    .map(CandleResponse::from)
                    .toList();
        }
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusSeconds(1);
        return multiIntervalCandleRepository
                .findByInstrumentSymbolAndCandleIntervalAndOpenTimeBetweenOrderByOpenTimeAsc(
                        symbol, interval, fromInstant, toInstant
                )
                .stream()
                .map(CandleResponse::from)
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
                .findByInstrumentSymbolOrderByTradeDateAsc(symbol)
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
    @Override
    public List<TrendMetricResponse> getTrendMetrics(String symbol) {
        return trendMetricRepository
                .findByInstrumentSymbolOrderByTradeDateAsc(symbol)
                .stream()
                .map(TrendMetricResponse::from)
                .toList();
    }
}