package com.company.finance_api.service;

import com.company.finance_api.dto.MarketOverviewPageResponse;
import com.company.finance_api.dto.MarketInsightsResponse;

public interface MarketOverviewService {

    MarketOverviewPageResponse getOverview(int page, int size, String category, String search, String targetCurrency);

    MarketInsightsResponse getInsights(String targetCurrency);
}
