package com.company.analytics.application;

import com.company.analytics.domain.enums.CandleInterval;
import com.company.analytics.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsQueryService {

    List<CandleResponse> getCandles(String symbol, LocalDate from, LocalDate to);
    List<MovingAverageResponse> getMovingAverage(String symbol);
    List<RSIResponse> getRSI(String symbol);
    List<TrendMetricResponse> getTrendMetrics(String symbol);
    List<CandleResponse> getCandlesByInterval(
            String symbol,
            CandleInterval interval,
            LocalDate from,
            LocalDate to
    );
}