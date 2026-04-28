package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.MarketInsightsResponse;
import com.company.finance_api.dto.MarketOverviewPageResponse;
import com.company.finance_api.service.MarketOverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/market")
public class MarketOverviewController {

    private final MarketOverviewService marketOverviewService;

    public MarketOverviewController(MarketOverviewService marketOverviewService) {
        this.marketOverviewService = marketOverviewService;
    }

    @GetMapping("/overview")
    public ApiResponse<MarketOverviewPageResponse> overview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "q") String search,
            @RequestHeader(value = "X-Currency", required = false) String currency
    ) {
        return ApiResponse.success(
                marketOverviewService.getOverview(page, size, category, search, currency)
        );
    }

    @GetMapping("/insights")
    public ApiResponse<MarketInsightsResponse> insights(
            @RequestHeader(value = "X-Currency", required = false) String currency
    ) {
        return ApiResponse.success(marketOverviewService.getInsights(currency));
    }
}
