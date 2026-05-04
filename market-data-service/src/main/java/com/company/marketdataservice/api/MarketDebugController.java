package com.company.marketdataservice.api;

import com.company.marketdataservice.event.MarketPriceUpdatedEvent;
import com.company.marketdataservice.historical.HistoricalPricePoint;
import com.company.marketdataservice.history.MarketPriceHistoryRepository;
import com.company.marketdataservice.provider.yahoo.YahooFinanceHistoricalPriceProvider;
import com.company.marketdataservice.provider.yahoo.YahooFinanceProvider;
import com.company.marketdataservice.service.history.MarketHistoryWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/market/debug")
@RequiredArgsConstructor
public class MarketDebugController {

    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final YahooFinanceProvider yahooFinanceProvider;
    private final YahooFinanceHistoricalPriceProvider yahooFinanceHistoricalPriceProvider;
    private final MarketHistoryWriteService marketHistoryWriteService;

    @GetMapping("/latest")
    public Mono<List<DebugPriceRow>> latest() {
        return Mono.fromCallable(() -> marketPriceHistoryRepository.findLatestDebugRows(20)
                        .stream()
                        .map(row -> new DebugPriceRow(row.getSymbol(), row.getObservedAt(), row.getPrice()))
                        .toList())
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/history/{symbol}")
    public Mono<List<DebugPriceRow>> history(@PathVariable String symbol) {
        return Mono.fromCallable(() -> {
                    String normalized = normalizeSymbol(symbol);
                    List<DebugPriceRow> existing = marketPriceHistoryRepository.findLatestDebugRowsBySymbol(normalized, 50)
                            .stream()
                            .map(row -> new DebugPriceRow(row.getSymbol(), row.getObservedAt(), row.getPrice()))
                            .toList();
                    if (!existing.isEmpty()) {
                        return existing;
                    }
                    List<HistoricalPricePoint> historical = yahooFinanceHistoricalPriceProvider.fetchRange(
                            normalized,
                            LocalDate.now().minusYears(1),
                            LocalDate.now()
                    );
                    if (!historical.isEmpty()) {
                        marketHistoryWriteService.saveBatch(historical.stream()
                                .map(point -> new MarketPriceUpdatedEvent(
                                        UUID.randomUUID().toString(),
                                        normalized,
                                        point.price(),
                                        point.priceType(),
                                        point.source(),
                                        point.occurredAt(),
                                        point.instrumentId()
                                ))
                                .toList());
                    }
                    return marketPriceHistoryRepository.findLatestDebugRowsBySymbol(normalized, 50)
                            .stream()
                            .map(row -> new DebugPriceRow(row.getSymbol(), row.getObservedAt(), row.getPrice()))
                            .toList();
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/yahoo/{symbol}")
    public Mono<YahooDebugResponse> yahoo(@PathVariable String symbol) {
        return Mono.fromCallable(() -> {
                    String normalized = normalizeSymbol(symbol);
                    BigDecimal price = yahooFinanceProvider.fetchPrice(normalized);
                    return new YahooDebugResponse(normalized, price, Instant.now(), yahooFinanceProvider.source());
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private static String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is blank");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    public record DebugPriceRow(
            String symbol,
            Instant observedAt,
            BigDecimal price
    ) {
    }

    public record YahooDebugResponse(
            String symbol,
            BigDecimal price,
            Instant timestamp,
            String source
    ) {
    }
}
