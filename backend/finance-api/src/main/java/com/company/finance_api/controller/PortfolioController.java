package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.PortfolioPositionResponse;
import com.company.finance_api.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ApiResponse<List<PortfolioPositionResponse>> myPortfolio() {
        return ApiResponse.success(
                portfolioService.getMyPortfolio()
        );
    }
}