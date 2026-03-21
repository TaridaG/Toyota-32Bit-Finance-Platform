package com.company.finance_api.service;

import com.company.finance_api.dto.PortfolioPositionResponse;
import com.company.finance_api.dto.PortfolioSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface PortfolioService {

    List<PortfolioPositionResponse> getMyPortfolio();
    PortfolioSummaryResponse getPortfolioSummary();
    PortfolioSummaryResponse getPortfolioSummary(UUID userId);
}