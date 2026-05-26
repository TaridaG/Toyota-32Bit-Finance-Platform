package com.company.finance_api.portfolio.application;

import com.company.finance_api.dto.PortfolioValuationResponse;

/** PortfolioValuationService iş mantığını uygular (portfolio valuation service). */
public interface PortfolioValuationService {

  /** getMyValuation sözleşmesi. */
  PortfolioValuationResponse getMyValuation();
}
