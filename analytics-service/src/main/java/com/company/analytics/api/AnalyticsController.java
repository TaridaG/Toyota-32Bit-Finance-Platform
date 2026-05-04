package com.company.analytics.api;

import com.company.analytics.application.AnalyticsQueryService;
import com.company.analytics.common.ApiResponse;
import com.company.analytics.domain.enums.CandleInterval;
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

    @GetMapping("/instruments/{symbol}/candles")
    public ApiResponse<List<CandleResponse>> getCandles(
            @PathVariable("symbol") String symbol,
            @RequestParam(name = "interval", required = false) CandleInterval interval,
            @RequestParam(name = "from", required = false) LocalDate from,
            @RequestParam(name = "to", required = false) LocalDate to
    ) {
        if (interval != null) {
            return ApiResponse.success(
                    analyticsQueryService.getCandlesByInterval(symbol, interval, from, to)
            );
        }
        return ApiResponse.success(
                analyticsQueryService.getCandles(symbol, from, to)
        );
    }
    @GetMapping("/instruments/{symbol}/moving-average")
    public ApiResponse<List<MovingAverageResponse>> getMovingAverage(
            @PathVariable("symbol") String symbol
    ){
        return ApiResponse.success(
                analyticsQueryService.getMovingAverage(symbol)
        );
    }
    @GetMapping("/instruments/{symbol}/rsi")
    public ApiResponse<List<RSIResponse>> getRSI(
            @PathVariable("symbol") String symbol
    ){
        return ApiResponse.success(
                analyticsQueryService.getRSI(symbol)
        );
    }
    @GetMapping("/instruments/{symbol}/trend")
    public ApiResponse<List<TrendMetricResponse>> getTrendMetrics(
            @PathVariable("symbol") String symbol
    ) {
        return ApiResponse.success(
                analyticsQueryService.getTrendMetrics(symbol)
        );
    }
}