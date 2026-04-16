package com.company.finance_api.portfolio.external.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.portfolio.external.dto.CreateExternalPortfolioRequest;
import com.company.finance_api.portfolio.external.dto.CreateExternalPositionRequest;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioAllocationResponse;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioResponse;
import com.company.finance_api.portfolio.external.dto.ExternalPortfolioSummaryResponse;
import com.company.finance_api.portfolio.external.service.ExternalPortfolioService;
import com.company.finance_api.portfolio.external.service.ExternalPortfolioValuationService;
import com.company.finance_api.security.CurrentUserResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/external/portfolios")
@RequiredArgsConstructor
public class ExternalPortfolioController {

    private final ExternalPortfolioService service;
    private final ExternalPortfolioValuationService valuationService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping
    public ApiResponse<ExternalPortfolioResponse> create(
            @Valid @RequestBody CreateExternalPortfolioRequest request
    ) {
        UUID userId = currentUserResolver.getCurrentUserId();
        return ApiResponse.success(service.createPortfolio(userId, request));
    }

    @GetMapping
    public ApiResponse<List<ExternalPortfolioResponse>> list() {
        UUID userId = currentUserResolver.getCurrentUserId();
        return ApiResponse.success(service.getUserPortfolios(userId));
    }

    @PostMapping("/{portfolioId}/positions")
    public ApiResponse<Void> addPosition(
            @PathVariable Long portfolioId,
            @Valid @RequestBody CreateExternalPositionRequest request
    ) {
        UUID userId = currentUserResolver.getCurrentUserId();
        service.addPosition(userId, portfolioId, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{portfolioId}/positions/{positionId}")
    public ApiResponse<Void> deletePosition(
            @PathVariable Long portfolioId,
            @PathVariable Long positionId
    ) {
        UUID userId = currentUserResolver.getCurrentUserId();
        service.deletePosition(userId, portfolioId, positionId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{portfolioId}/summary")
    public ApiResponse<ExternalPortfolioSummaryResponse> summary(@PathVariable Long portfolioId) {
        UUID userId = currentUserResolver.getCurrentUserId();
        return ApiResponse.success(valuationService.calculateSummary(userId, portfolioId));
    }

    @GetMapping("/{portfolioId}/allocation")
    public ApiResponse<List<ExternalPortfolioAllocationResponse>> allocation(@PathVariable Long portfolioId) {
        UUID userId = currentUserResolver.getCurrentUserId();
        return ApiResponse.success(valuationService.calculateAllocation(userId, portfolioId));
    }
}

