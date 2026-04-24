package com.company.finance_api.controller;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.dto.*;
import com.company.finance_api.service.ChartService;
import com.company.finance_api.web.PriceTypeParamResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/charts")
@RequiredArgsConstructor
public class ChartController {

    private final ChartService chartService;

    @GetMapping("/{instrumentId}/candles")
    public ApiResponse<List<CandlestickResponse>> candles(
            @PathVariable Long instrumentId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) String priceType
    ) {
        return ApiResponse.success(
                chartService.getCandlesticks(
                        instrumentId,
                        from,
                        to,
                        PriceTypeParamResolver.resolve(priceType)
                )
        );
    }

    @GetMapping("/{instrumentId}/trades")
    public ApiResponse<List<TradeMarkerResponse>> trades(
            @PathVariable Long instrumentId
    ) {
        return ApiResponse.success(
                chartService.getMyTrades(instrumentId)
        );
    }

    @GetMapping("/{instrumentId}/alarms")
    public ApiResponse<List<AlarmLineResponse>> alarms(
            @PathVariable Long instrumentId
    ) {
        return ApiResponse.success(
                chartService.getMyAlarms(instrumentId)
        );
    }
}