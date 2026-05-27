package com.company.marketdataservice.history.infrastructure.http;
import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceSummaryDto;
import com.company.marketdataservice.history.application.HistoricalMarketDataReadService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Locale;

/**
 * `geçmiş veri ve backfill` REST endpoint'lerini expose eden HTTP controller.
 */
@RestController
@RequestMapping("/api/v1/market")
public class MarketHistoryController {

    private final HistoricalMarketDataReadService historicalMarketDataReadService;

    public MarketHistoryController(HistoricalMarketDataReadService historicalMarketDataReadService) {
        this.historicalMarketDataReadService = historicalMarketDataReadService;
    }

    @GetMapping("/prices/history")
    public List<HistoryPointDto> priceHistory(
            @RequestParam String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return historicalMarketDataReadService.getPriceHistory(symbol, from, to);
    }

    @GetMapping("/prices/summary")
    public Map<String, MarketPriceSummaryDto> pricesSummary(@RequestParam String symbols) {
        List<String> parsed = Arrays.stream(symbols.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .toList();
        return historicalMarketDataReadService.getPriceSummary(parsed);
    }

    @GetMapping("/fx/history")
    public List<HistoryPointDto> fxHistory(
            @RequestParam String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return historicalMarketDataReadService.getFxHistory(symbol, from, to);
    }

    @GetMapping("/funds/history")
    public List<HistoryPointDto> fundHistory(
            @RequestParam String fundCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return historicalMarketDataReadService.getFundHistory(fundCode, from, to);
    }
}
