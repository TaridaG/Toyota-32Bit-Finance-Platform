package com.company.analytics.query.infrastructure.http;

import com.company.analytics.query.application.AnalyticsQueryService;
import com.company.analytics.shared.web.ApiResponse;
import com.company.analytics.processing.domain.enums.CandleInterval;
import com.company.analytics.query.infrastructure.http.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Analytics query REST controller'ı.
 * Enstrüman bazlı candle, moving average, RSI ve trend metric endpoint'lerini sunar.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsQueryService analyticsQueryService;

    /**
     * Enstrüman için candle verilerini döner.
     * {@code interval} parametresi verilirse çoklu interval candle query'si yapılır;
     * aksi halde günlük candle kayıtları döner.
     *
     * @param symbol   enstrüman sembolü
     * @param interval opsiyonel candle interval
     * @param from     opsiyonel başlangıç tarihi
     * @param to       opsiyonel bitiş tarihi
     * @return candle DTO listesini içeren standart API yanıtı
     */
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

    /**
     * Enstrüman için moving average (MA7, MA30, MA90) değerlerini döner.
     *
     * @param symbol enstrüman sembolü
     * @return moving average DTO listesini içeren standart API yanıtı
     */
    @GetMapping("/instruments/{symbol}/moving-average")
    public ApiResponse<List<MovingAverageResponse>> getMovingAverage(
            @PathVariable("symbol") String symbol
    ){
        return ApiResponse.success(
                analyticsQueryService.getMovingAverage(symbol)
        );
    }

    /**
     * Enstrüman için RSI-14 değerlerini döner.
     *
     * @param symbol enstrüman sembolü
     * @return RSI DTO listesini içeren standart API yanıtı
     */
    @GetMapping("/instruments/{symbol}/rsi")
    public ApiResponse<List<RSIResponse>> getRSI(
            @PathVariable("symbol") String symbol
    ){
        return ApiResponse.success(
                analyticsQueryService.getRSI(symbol)
        );
    }

    /**
     * Enstrüman için trend metric kayıtlarını döner.
     *
     * @param symbol enstrüman sembolü
     * @return trend metric DTO listesini içeren standart API yanıtı
     */
    @GetMapping("/instruments/{symbol}/trend")
    public ApiResponse<List<TrendMetricResponse>> getTrendMetrics(
            @PathVariable("symbol") String symbol
    ) {
        return ApiResponse.success(
                analyticsQueryService.getTrendMetrics(symbol)
        );
    }
}
