package com.company.analytics.application;

import com.company.analytics.dto.AnalyticsSummaryResponse;
import com.company.analytics.dto.CandleResponse;

import java.time.LocalDate;
import java.util.List;

public interface AnalyticsQueryService {

    List<AnalyticsSummaryResponse> getDaily(String symbol);
    List<CandleResponse> getCandles(String symbol, LocalDate from, LocalDate to);

}