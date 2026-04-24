package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.domain.PortfolioSnapshot;
import com.company.finance_api.dto.PortfolioPositionResponse;
import com.company.finance_api.dto.PortfolioSummaryResponse;
import com.company.finance_api.dto.PortfolioValuationResponse;
import com.company.finance_api.service.PortfolioService;
import com.company.finance_api.service.PortfolioSnapshotService;
import com.company.finance_api.service.PortfolioValuationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final PortfolioSnapshotService portfolioSnapshotService;
    private final PortfolioValuationService portfolioValuationService;

    @GetMapping
    public ApiResponse<List<PortfolioPositionResponse>> myPortfolio() {
        return ApiResponse.success(
                portfolioService.getMyPortfolio()
        );
    }
    @GetMapping("/summary")
    public ApiResponse<PortfolioSummaryResponse> summary() {
        return ApiResponse.success(
                portfolioService.getPortfolioSummary()
        );
    }

    @GetMapping("/valuation")
    public ApiResponse<PortfolioValuationResponse> valuation() {
        return ApiResponse.success(portfolioValuationService.getMyValuation());
    }
    @GetMapping("/snapshots")
    public ApiResponse<List<PortfolioSnapshot>> snapshots() {
        return ApiResponse.success(portfolioSnapshotService.getMySnapshots());
    }
}