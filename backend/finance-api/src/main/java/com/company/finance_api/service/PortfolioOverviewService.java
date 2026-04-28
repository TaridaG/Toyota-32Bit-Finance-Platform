package com.company.finance_api.service;

import com.company.finance_api.dto.PortfolioOverviewResponse;

public interface PortfolioOverviewService {
    PortfolioOverviewResponse getMyOverview(String targetCurrency);
}
