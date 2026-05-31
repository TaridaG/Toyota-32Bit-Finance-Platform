package com.company.finance_api.portfolio.application;

import com.company.finance_api.portfolio.infrastructure.http.dto.PortfolioPerformanceSeriesResponse;
import java.util.UUID;

/** Daily portfolio value and performance chart service. */
public interface PortfolioPerformanceSeriesService {

  PortfolioPerformanceSeriesResponse getMyPerformanceSeries(
      String targetCurrency, Long portfolioId, String range);

  void recomputePortfolioHistory(UUID userId, Long portfolioId);

  void recomputeAllPortfolioHistory();
}
