package com.company.analytics.api;

import com.company.analytics.application.AnalyticsQueryService;
import com.company.analytics.common.ApiResponse;
import com.company.analytics.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    @GetMapping("/instruments/{symbol}/daily")
    public ApiResponse<List<AnalyticsSummaryResponse>> getDaily(
            @PathVariable String symbol
    ) {
        return ApiResponse.success(analyticsQueryService.getDaily(symbol));
    }

    @GetMapping("/instruments/{symbol}/candles")
    public ApiResponse<List<CandleResponse>> getCandles(
            @PathVariable String symbol,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return ApiResponse.success(
                analyticsQueryService.getCandles(symbol, from, to)
        );
    }
    @GetMapping("/instruments/{symbol}/moving-average")
    public ApiResponse<List<MovingAverageResponse>> getMovingAverage(
            @PathVariable String symbol
    ){
        return ApiResponse.success(
                analyticsQueryService.getMovingAverage(symbol)
        );
    }
    @GetMapping("/instruments/{symbol}/vwap")
    public ApiResponse<List<VWAPResponse>> getVWAP(@PathVariable String symbol) {
        return ApiResponse.success(analyticsQueryService.getVWAP(symbol));
    }
    @GetMapping("/instruments/{symbol}/rsi")
    public ApiResponse<List<RSIResponse>> getRSI(
            @PathVariable String symbol
    ){
        return ApiResponse.success(
                analyticsQueryService.getRSI(symbol)
        );
    }
}