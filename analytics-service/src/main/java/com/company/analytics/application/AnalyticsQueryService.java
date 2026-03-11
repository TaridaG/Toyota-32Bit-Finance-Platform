package com.company.analytics.application;

import com.company.analytics.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsQueryService {

    List<AnalyticsSummaryResponse> getDaily(String symbol);
    List<CandleResponse> getCandles(String symbol, LocalDate from, LocalDate to);
    List<VWAPResponse> getVWAP(String symbol);
    List<MovingAverageResponse> getMovingAverage(String symbol);
    List<RSIResponse> getRSI(String symbol);
}