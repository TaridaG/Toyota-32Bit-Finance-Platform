package com.company.marketdataservice.fundamentals.infrastructure.http;
import com.company.marketdataservice.fundamentals.infrastructure.http.dto.InstrumentFundamentalsDto;
import com.company.marketdataservice.fundamentals.application.InstrumentFundamentalsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * `temel veri (fundamentals)` REST endpoint'lerini expose eden HTTP controller.
 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketFundamentalsController {

    private final InstrumentFundamentalsService instrumentFundamentalsService;

    @GetMapping("/instruments/{symbol}/fundamentals")
    public Mono<InstrumentFundamentalsDto> fundamentals(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) {
        // Service currently performs blocking provider I/O (Finnhub). Run off event-loop.
        return Mono.fromCallable(() -> instrumentFundamentalsService.getFundamentals(symbol, forceRefresh))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
