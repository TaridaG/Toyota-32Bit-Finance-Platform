package com.company.marketdataservice.service;

import com.company.marketdataservice.catalog.MarketCatalogSegmentRules;
import com.company.marketdataservice.dto.FxRateDto;
import com.company.marketdataservice.dto.HistoryPointDto;
import com.company.marketdataservice.dto.MarketPriceDto;
import com.company.marketdataservice.dto.MarketPriceSummaryDto;
import com.company.marketdataservice.dto.MarketSegmentPulseOverallDto;
import com.company.marketdataservice.dto.MarketSegmentPulseResponse;
import com.company.marketdataservice.dto.MarketSegmentPulseRowDto;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MarketSegmentPulseService {

    private static final Logger log = LoggerFactory.getLogger(MarketSegmentPulseService.class);

    private static final List<String> SEGMENT_ORDER =
            List.of("crypto", "bist", "nasdaq", "forex", "metals", "globalFutures", "funds", "bonds");

    private static final int SUMMARY_CHUNK = 40;
    private static final long CACHE_TTL_MS = 30_000L;

    private final MarketDataReadService marketDataReadService;
    private final HistoricalMarketDataReadService historicalMarketDataReadService;

    private final AtomicReference<CacheEntry> cache = new AtomicReference<>();

    public MarketSegmentPulseService(
            MarketDataReadService marketDataReadService,
            HistoricalMarketDataReadService historicalMarketDataReadService) {
        this.marketDataReadService = marketDataReadService;
        this.historicalMarketDataReadService = historicalMarketDataReadService;
    }

    public MarketSegmentPulseResponse getPulse() {
        long nowMs = System.currentTimeMillis();
        CacheEntry existing = cache.get();
        if (existing != null && existing.expiresAtMs > nowMs) {
            return existing.response;
        }
        MarketSegmentPulseResponse computed = computePulse();
        cache.set(new CacheEntry(computed, nowMs + CACHE_TTL_MS));
        return computed;
    }

    private MarketSegmentPulseResponse computePulse() {
        long t0 = System.currentTimeMillis();
        List<MarketPriceDto> prices = marketDataReadService.getLatestPrices();
        List<FxRateDto> fx = marketDataReadService.getFxRates();

        record Row(String symbol, String source, String wireCategory, String segment) {}

        Map<String, Row> unique = new LinkedHashMap<>();
        for (MarketPriceDto p : prices) {
            if (p.symbol() == null || p.symbol().isBlank()) {
                continue;
            }
            String sym = p.symbol().trim().toUpperCase(Locale.ROOT);
            String wire = MarketCatalogSegmentRules.inferWireCategory(sym);
            String seg = MarketCatalogSegmentRules.pulseSegment(sym, wire, p.source());
            if (seg != null) {
                unique.put(sym, new Row(sym, p.source(), wire, seg));
            }
        }
        for (FxRateDto f : fx) {
            if (f.symbol() == null || f.symbol().isBlank()) {
                continue;
            }
            String sym = f.symbol().trim().toUpperCase(Locale.ROOT);
            String wire = "FX";
            String seg = MarketCatalogSegmentRules.pulseSegment(sym, wire, f.source());
            if (seg != null) {
                unique.putIfAbsent(sym, new Row(sym, f.source(), wire, seg));
            }
        }
        List<Row> rows = new ArrayList<>(unique.values());

        Map<String, Double> change1dBySymbol = new LinkedHashMap<>();
        List<Row> summaryPath = new ArrayList<>();
        List<Row> fxPath = new ArrayList<>();
        for (Row r : rows) {
            if (MarketCatalogSegmentRules.usesFxHistoryPath(r.symbol(), r.wireCategory())) {
                fxPath.add(r);
            } else {
                summaryPath.add(r);
            }
        }

        for (int i = 0; i < summaryPath.size(); i += SUMMARY_CHUNK) {
            int end = Math.min(summaryPath.size(), i + SUMMARY_CHUNK);
            List<String> chunk = summaryPath.subList(i, end).stream().map(Row::symbol).distinct().toList();
            Map<String, MarketPriceSummaryDto> batch =
                    historicalMarketDataReadService.getPriceSummary(chunk);
            for (String sym : chunk) {
                MarketPriceSummaryDto dto = batch.get(sym);
                if (dto != null) {
                    change1dBySymbol.put(sym, dto.change1D());
                }
            }
        }

        LocalDate to = LocalDate.now(ZoneOffset.UTC);
        LocalDate from = to.minusDays(6);
        for (Row r : fxPath) {
            double pct = fxPercentChange1d(r.symbol(), from, to);
            change1dBySymbol.put(r.symbol(), pct);
        }

        List<Double> allChanges = new ArrayList<>();
        int advancingAll = 0;
        for (Row r : rows) {
            Double ch = change1dBySymbol.get(r.symbol());
            if (ch == null || !Double.isFinite(ch)) {
                continue;
            }
            allChanges.add(ch);
            if (ch > 0d) {
                advancingAll++;
            }
        }
        MarketSegmentPulseOverallDto overall;
        if (allChanges.isEmpty()) {
            overall = new MarketSegmentPulseOverallDto(null, 0, 0);
        } else {
            double sumAll = 0d;
            for (Double v : allChanges) {
                sumAll += v;
            }
            overall = new MarketSegmentPulseOverallDto(sumAll / allChanges.size(), allChanges.size(), advancingAll);
        }

        Map<String, List<Double>> bySegment = new LinkedHashMap<>();
        for (String s : SEGMENT_ORDER) {
            bySegment.put(s, new ArrayList<>());
        }
        for (Row r : rows) {
            Double ch = change1dBySymbol.get(r.symbol());
            if (ch == null || !Double.isFinite(ch)) {
                continue;
            }
            bySegment.computeIfAbsent(r.segment(), k -> new ArrayList<>()).add(ch);
        }

        List<MarketSegmentPulseRowDto> out = new ArrayList<>();
        for (String seg : SEGMENT_ORDER) {
            List<Double> vals = bySegment.getOrDefault(seg, List.of());
            if (vals.isEmpty()) {
                out.add(new MarketSegmentPulseRowDto(seg, null, 0, 0));
                continue;
            }
            double sum = 0d;
            int adv = 0;
            for (Double v : vals) {
                sum += v;
                if (v > 0d) {
                    adv++;
                }
            }
            double mean = sum / vals.size();
            out.add(new MarketSegmentPulseRowDto(seg, mean, vals.size(), adv));
        }

        log.info(
                "MARKET_SEGMENT_PULSE rows={} summaryPath={} fxPath={} overallN={} durationMs={}",
                rows.size(),
                summaryPath.size(),
                fxPath.size(),
                overall.count(),
                System.currentTimeMillis() - t0);
        return new MarketSegmentPulseResponse(overall, out, Instant.now());
    }

    private double fxPercentChange1d(String symbol, LocalDate from, LocalDate to) {
        List<HistoryPointDto> history = historicalMarketDataReadService.getFxHistory(symbol, from, to);
        return percentChangeFirstToLast(history);
    }

    /** Same first→last % math as price summary history windows in this service module. */
    private static double percentChangeFirstToLast(List<HistoryPointDto> history) {
        if (history == null || history.size() < 2) {
            return 0d;
        }
        BigDecimal first = history.get(0).value();
        BigDecimal last = history.get(history.size() - 1).value();
        if (first == null || last == null || first.compareTo(BigDecimal.ZERO) == 0) {
            return 0d;
        }
        return last.subtract(first)
                .divide(first, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private record CacheEntry(MarketSegmentPulseResponse response, long expiresAtMs) {}
}
