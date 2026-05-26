package com.company.finance_api.portfolio.application;

import com.company.finance_api.dto.PortfolioPositionResponse;
import com.company.finance_api.dto.PortfolioSummaryResponse;
import java.util.List;
import java.util.UUID;

/** PortfolioService iş mantığını uygular (portfolio service). */
public interface PortfolioService {

  /** getMyPortfolio sözleşmesi. */
  List<PortfolioPositionResponse> getMyPortfolio();

  /** getPortfolioSummary sözleşmesi. */
  PortfolioSummaryResponse getPortfolioSummary();

  /** getPortfolioSummary sözleşmesi. */
  PortfolioSummaryResponse getPortfolioSummary(UUID userId);
}
