package com.company.finance_api.chart.infrastructure.http;

import com.company.finance_api.alarm.infrastructure.http.dto.AlarmLineResponse;
import com.company.finance_api.chart.application.ChartService;
import com.company.finance_api.chart.infrastructure.http.dto.CandlestickResponse;
import com.company.finance_api.chart.infrastructure.http.dto.TradeMarkerResponse;
import com.company.finance_api.shared.web.ApiResponse;
import com.company.finance_api.shared.web.PriceTypeParamResolver;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** Mum grafik (candlestick) ve trade marker REST endpoint'lerini sunar. */
@RestController
@RequestMapping("/api/charts")
@RequiredArgsConstructor
public class ChartController {

  private final ChartService chartService;

  /** HTTP handler — `/{instrumentId}/candles` endpoint'i. */
  @GetMapping("/{instrumentId}/candles")
  public ApiResponse<List<CandlestickResponse>> candles(
      @PathVariable Long instrumentId,
      @RequestParam Instant from,
      @RequestParam Instant to,
      @RequestParam(required = false) String priceType) {
    return ApiResponse.success(
        chartService.getCandlesticks(
            instrumentId, from, to, PriceTypeParamResolver.resolve(priceType)));
  }

  /** HTTP handler — `/{instrumentId}/trades` endpoint'i. */
  @GetMapping("/{instrumentId}/trades")
  public ApiResponse<List<TradeMarkerResponse>> trades(@PathVariable Long instrumentId) {
    return ApiResponse.success(chartService.getMyTrades(instrumentId));
  }

  /** HTTP handler — `/{instrumentId}/alarms` endpoint'i. */
  @GetMapping("/{instrumentId}/alarms")
  public ApiResponse<List<AlarmLineResponse>> alarms(@PathVariable Long instrumentId) {
    return ApiResponse.success(chartService.getMyAlarms(instrumentId));
  }
}
