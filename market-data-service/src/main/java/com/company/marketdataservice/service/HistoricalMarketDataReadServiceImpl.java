package com.company.marketdataservice.service;

import com.company.marketdataservice.dto.HistoryPointDto;
import com.company.marketdataservice.dto.MarketPriceSummaryDto;
import com.company.marketdataservice.history.FundNavHistoryRepository;
import com.company.marketdataservice.history.FxRateHistoryRepository;
import com.company.marketdataservice.history.MarketPriceHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class HistoricalMarketDataReadServiceImpl implements HistoricalMarketDataReadService {

    private static final long MAX_RANGE_DAYS = 365L;
    private static final Logger log = LoggerFactory.getLogger(HistoricalMarketDataReadServiceImpl.class);

    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final FxRateHistoryRepository fxRateHistoryRepository;
    private final FundNavHistoryRepository fundNavHistoryRepository;

    public HistoricalMarketDataReadServiceImpl(
            MarketPriceHistoryRepository marketPriceHistoryRepository,
            FxRateHistoryRepository fxRateHistoryRepository,
            FundNavHistoryRepository fundNavHistoryRepository
    ) {
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
        this.fxRateHistoryRepository = fxRateHistoryRepository;
        this.fundNavHistoryRepository = fundNavHistoryRepository;
    }

    @Override
    public List<HistoryPointDto> getPriceHistory(String symbol, LocalDate from, LocalDate to) {
        if (!isValid(symbol, from, to)) {
            return List.of();
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        Instant fromInclusive = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return marketPriceHistoryRepository.findHistoryPoints(normalized, fromInclusive, toExclusive);
    }

    @Override
    public List<HistoryPointDto> getFxHistory(String symbol, LocalDate from, LocalDate to) {
        if (!isValid(symbol, from, to)) {
            return List.of();
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        Instant fromInclusive = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return fxRateHistoryRepository.findHistoryPoints(normalized, fromInclusive, toExclusive);
    }

    @Override
    public List<HistoryPointDto> getFundHistory(String fundCode, LocalDate from, LocalDate to) {
        if (!isValid(fundCode, from, to)) {
            return List.of();
        }
        String normalized = fundCode.trim().toUpperCase(Locale.ROOT);
        Instant fromInclusive = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return fundNavHistoryRepository.findHistoryPoints(normalized, fromInclusive, toExclusive);
    }

    @Override
    public Map<String, MarketPriceSummaryDto> getPriceSummary(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Map.of();
        }

        Map<String, MarketPriceSummaryDto> out = new LinkedHashMap<>();
        Instant now = Instant.now();
        Instant toExclusive = now.plus(1, ChronoUnit.DAYS);
        int validSymbols = 0;
        for (String raw : symbols) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String symbol = raw.trim().toUpperCase(Locale.ROOT);
            List<HistoryPointDto> latestPoints = marketPriceHistoryRepository.findLatestHistoryPoint(symbol, PageRequest.of(0, 1));
            if (latestPoints.isEmpty() || latestPoints.get(0).value() == null) {
                continue;
            }
            validSymbols++;
            BigDecimal latestPrice = latestPoints.get(0).value();

            double change1D = computePeriodChange(symbol, now.minus(1, ChronoUnit.DAYS), toExclusive);
            double change1M = computePeriodChange(symbol, now.minus(30, ChronoUnit.DAYS), toExclusive);
            double change3M = computePeriodChange(symbol, now.minus(90, ChronoUnit.DAYS), toExclusive);
            double change6M = computePeriodChange(symbol, now.minus(180, ChronoUnit.DAYS), toExclusive);
            double change1Y = computePeriodChange(symbol, now.minus(365, ChronoUnit.DAYS), toExclusive);

            out.put(symbol, new MarketPriceSummaryDto(latestPrice, change1D, change1M, change3M, change6M, change1Y));
        }
        log.info("MARKET_DB_SUMMARY_READ symbolsRequested={} symbolsResolved={}", symbols.size(), validSymbols);
        return out;
    }

    private double computePeriodChange(String symbol, Instant fromInclusive, Instant toExclusive) {
        List<HistoryPointDto> history = marketPriceHistoryRepository.findHistoryPoints(symbol, fromInclusive, toExclusive);
        if (history.size() < 2) {
            return 0d;
        }
        BigDecimal first = history.get(0).value();
        BigDecimal last = history.get(history.size() - 1).value();
        if (first == null || last == null || first.compareTo(BigDecimal.ZERO) == 0) {
            return 0d;
        }
        return last.subtract(first)
                .divide(first, 8, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private static boolean isValid(String symbolOrCode, LocalDate from, LocalDate to) {
        if (symbolOrCode == null || symbolOrCode.isBlank() || from == null || to == null || to.isBefore(from)) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        return days <= MAX_RANGE_DAYS;
    }
}
