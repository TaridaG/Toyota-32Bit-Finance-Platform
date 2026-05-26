package com.company.finance_api.portfolio.application;

import com.company.finance_api.dto.PortfolioOverviewResponse;

/** PortfolioOverviewService iş mantığını uygular (portfolio overview service). */
public interface PortfolioOverviewService {
  /** getMyOverview sözleşmesi. */
  PortfolioOverviewResponse getMyOverview(String targetCurrency, Long portfolioId);
}
