package com.company.analytics.api;

import com.company.analytics.application.AnalyticsQueryService;
import com.company.analytics.dto.AnalyticsSummaryResponse;
import com.company.analytics.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsQueryService queryService;

    @GetMapping("/instruments/{symbol}/daily")
    public ApiResponse<List<AnalyticsSummaryResponse>> getDaily(
            @PathVariable String symbol
    ) {
        return ApiResponse.success(
                queryService.getDaily(symbol)
        );
    }
}