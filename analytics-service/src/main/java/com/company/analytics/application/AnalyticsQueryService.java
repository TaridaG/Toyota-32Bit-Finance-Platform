package com.company.analytics.application;

import com.company.analytics.dto.AnalyticsSummaryResponse;

import java.util.List;

public interface AnalyticsQueryService {

    List<AnalyticsSummaryResponse> getDaily(String symbol);

}