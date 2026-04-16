package com.company.finance_api.portfolio.external.service;

import com.company.finance_api.portfolio.external.dto.ExternalPortfolioAllocationResponse;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface ExternalPortfolioValuationService {

    ExternalPortfolioSummaryResponse calculateSummary(UUID userId, Long portfolioId);

    List<ExternalPortfolioAllocationResponse> calculateAllocation(UUID userId, Long portfolioId);
}

