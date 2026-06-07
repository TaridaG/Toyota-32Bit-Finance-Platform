package com.company.finance_api.portfolio.application;

import com.company.finance_api.portfolio.infrastructure.http.dto.PortfolioTradeFlowResponse;

/** PortfolioTradeFlowService iş mantığını uygular (portfolio trade flow service). */
public interface PortfolioTradeFlowService {

  /** Hedef para biriminde alım/satım akışı (trade flow) zaman serisini döner. */
  PortfolioTradeFlowResponse getMyTradeFlow(String targetCurrency, Long portfolioId);
}
