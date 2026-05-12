package com.company.finance_api.portfolio.external.service;

import com.company.finance_api.portfolio.external.dto.CreateExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.dto.CreateExternalPositionRequest;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioResponse;
import com.company.finance_api.portfolio.external.dto.PatchExternalPortfolioRequest;

import java.util.List;
import java.util.UUID;

public interface ExternalPortfolioService {

    ExternalPortfolioResponse createPortfolio(UUID userId, CreateExternalPortfolioRequest request);

    List<ExternalPortfolioResponse> getUserPortfolios(UUID userId);

    ExternalPortfolioResponse getPortfolio(UUID userId, Long portfolioId);

    ExternalPortfolioResponse patchPortfolio(UUID userId, Long portfolioId, PatchExternalPortfolioRequest request);

    void deletePortfolio(UUID userId, Long portfolioId);

    void addPosition(UUID userId, Long portfolioId, CreateExternalPositionRequest request);

    void deletePosition(UUID userId, Long portfolioId, Long positionId);
}

