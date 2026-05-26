package com.company.finance_api.portfolio.application;

import com.company.finance_api.dto.PortfolioTradeFlowResponse;

/** PortfolioTradeFlowService iş mantığını uygular (portfolio trade flow service). */
public interface PortfolioTradeFlowService {

  /** getMyTradeFlow sözleşmesi. */
  PortfolioTradeFlowResponse getMyTradeFlow(String targetCurrency, Long portfolioId);
}
