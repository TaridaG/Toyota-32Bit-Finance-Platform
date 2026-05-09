package com.company.finance_api.service;

import com.company.finance_api.dto.PortfolioTradeFlowResponse;

public interface PortfolioTradeFlowService {

    PortfolioTradeFlowResponse getMyTradeFlow(String targetCurrency, Long portfolioId);
}
