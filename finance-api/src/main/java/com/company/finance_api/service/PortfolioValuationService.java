package com.company.finance_api.service;

import com.company.finance_api.dto.PortfolioValuationResponse;

/** PortfolioValuationService iş mantığını uygular (portfolio valuation service). */
public interface PortfolioValuationService {

  /** getMyValuation sözleşmesi. */
  PortfolioValuationResponse getMyValuation();
}
