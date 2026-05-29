package com.company.marketdataservice.history.application;
import com.company.marketdataservice.catalog.domain.MarketCatalogSegmentRules;
import com.company.marketdataservice.history.infrastructure.http.dto.HistoryPointDto;
import com.company.marketdataservice.spot.application.MarketDataReadService;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceSummaryDto;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryEntry;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.FxRateHistoryRepository;
import com.company.marketdataservice.fund.infrastructure.provider.TefasFundNavHistoryRehydrationService;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository.DebugHistoryRowView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geçmiş fiyat, FX ve fon NAV serilerini okuyup özet metrik üretir.
 */
@Service
public class HistoricalMarketDataReadServiceImpl implements HistoricalMarketDataReadService {

    private static final long MAX_RANGE_DAYS = 365L;
    /**
     * Inclusive calendar span for TRBOND charts (e.g. "last 5 years" from the UI). {@code DAYS.between + 1}
     * for five anniversaries is often 1827 (leap years), so 1826 was too tight and returned empty history
     * while {@link #getPriceSummary} still showed the latest print.
     */
    private static final long MAX_TRBOND_RANGE_DAYS = 2000L;
    private static final String MARKET_PRICE_TYPE = "MARKET";
    private static final Logger log = LoggerFactory.getLogger(HistoricalMarketDataReadServiceImpl.class);
    private static final ZoneId TURKEY = ZoneId.of("Europe/Istanbul");
    /** Max calendar slip when baseline NAV is older than the requested lookback (data gaps). */
    private static final long FUND_NAV_TRAILING_MAX_SLIP_DAYS = 12L;
    private static final long DEFAULT_SUMMARY_CACHE_TTL_MS = 45_000L;

    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final FxRateHistoryRepository fxRateHistoryRepository;
    private final FundNavHistoryRepository fundNavHistoryRepository;
    private final TefasFundNavHistoryRehydrationService fundNavHistoryRehydrationService;
    private final MarketDataReadService marketDataReadService;
    private final Clock clock;
    private final ConcurrentHashMap<String, SummaryCacheEntry> summaryCache = new ConcurrentHashMap<>();

    @Value("${market.summary.cache-ttl-ms:" + DEFAULT_SUMMARY_CACHE_TTL_MS + "}")
    private long summaryCacheTtlMs;

    public HistoricalMarketDataReadServiceImpl(
            MarketPriceHistoryRepository marketPriceHistoryRepository,
            FxRateHistoryRepository fxRateHistoryRepository,
            FundNavHistoryRepository fundNavHistoryRepository,
            @Autowired(required = false) TefasFundNavHistoryRehydrationService fundNavHistoryRehydrationService,
            @Autowired(required = false) MarketDataReadService marketDataReadService,
            @Autowired(required = false) Clock clock
    ) {
        this.marketPriceHistoryRepository = marketPriceHistoryRepository;
        this.fxRateHistoryRepository = fxRateHistoryRepository;
        this.fundNavHistoryRepository = fundNavHistoryRepository;
        this.fundNavHistoryRehydrationService = fundNavHistoryRehydrationService;
        this.marketDataReadService = marketDataReadService;
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    /**
     * Veriyi okur ve döner.
         * @param symbol enstrüman sembolü
         * @param from geçiş durumu
         * @param to geçiş durumu
         * @return işlem sonucu
         */
    @Override
    public List<HistoryPointDto> getPriceHistory(String symbol, LocalDate from, LocalDate to) {
        if (!isValid(symbol, from, to)) {
            return List.of();
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);
        Instant fromInclusive = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (normalized.startsWith("FUND_")) {
            String fundCode = normalized.substring("FUND_".length());
            return fundNavHistoryRepository.findHistoryPoints(fundCode, fromInclusive, toExclusive);
        }
        if (usesFxRateHistory(normalized)) {
            return fxRateHistoryRepository.findHistoryPoints(normalized, fromInclusive, toExclusive);
        }
        return marketPriceHistoryRepository.findHistoryPoints(normalized, fromInclusive, toExclusive);
    }

    /**
     * Veriyi okur ve döner.
         * @param symbol enstrüman sembolü
         * @param from geçiş durumu
         * @param to geçiş durumu
         * @return işlem sonucu
         */
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

    /**
     * Veriyi okur ve döner.
         * @param fundCode girdi parametresi
         * @param from geçiş durumu
         * @param to geçiş durumu
         * @return işlem sonucu
         */
    @Override
    public List<HistoryPointDto> getFundHistory(String fundCode, LocalDate from, LocalDate to) {
        if (!isValid(fundCode, from, to)) {
            return List.of();
        }
        String normalized = normalizeFundCode(fundCode);
        Instant fromInclusive = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<HistoryPointDto> points = fundNavHistoryRepository.findHistoryPoints(normalized, fromInclusive, toExclusive);
        if (fundNavHistoryRehydrationService != null && shouldRepairFundNavGaps(points, from, to)) {
            fundNavHistoryRehydrationService.scheduleRepairInteriorGapsInRange(normalized, from, to);
        }
        return points;
    }

    @Override
    public Map<String, MarketPriceSummaryDto> getPriceSummary(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Map.of();
        }
        List<String> normalized = normalizeSymbols(symbols);
        if (normalized.isEmpty()) {
            return Map.of();
        }
        long startedAt = System.nanoTime();
        Instant now = clock.instant();
        Map<String, MarketPriceSummaryDto> out = new LinkedHashMap<>();
        List<String> cacheMisses = new ArrayList<>();
        int cacheHits = 0;
        for (String symbol : normalized) {
            SummaryCacheEntry cached = summaryCache.get(symbol);
            if (cached != null && cached.expiresAt().isAfter(now)) {
                cacheHits++;
                if (cached.summary() != null) {
                    out.put(symbol, cached.summary());
                }
                continue;
            }
            cacheMisses.add(symbol);
        }
        if (!cacheMisses.isEmpty()) {
            out.putAll(loadAndCacheSummaries(cacheMisses, now));
        }
        long durationMs = nanosToMillis(startedAt);
        double avgSymbolMs = cacheMisses.isEmpty() ? 0d : durationMs / (double) cacheMisses.size();
        log.info(
                "MARKET_DB_SUMMARY_READ symbolsRequested={} symbolsResolved={} cacheHits={} cacheMisses={} avgMissMs={} durationMs={}",
                normalized.size(),
                out.size(),
                cacheHits,
                cacheMisses.size(),
                BigDecimal.valueOf(avgSymbolMs).setScale(2, RoundingMode.HALF_UP),
                durationMs);
        return out;
    }

    @Scheduled(
            initialDelayString = "${market.summary.cache-warm.initial-delay-ms:15000}",
            fixedDelayString = "${market.summary.cache-warm.fixed-delay-ms:" + DEFAULT_SUMMARY_CACHE_TTL_MS + "}")
    void warmSummaryCache() {
        if (marketDataReadService == null) {
            return;
        }
        LinkedHashSet<String> symbols = new LinkedHashSet<>();
        marketDataReadService.getLatestPrices().stream()
                .map(price -> price.symbol())
                .filter(Objects::nonNull)
                .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
                .filter(symbol -> !symbol.isBlank())
                .forEach(symbols::add);
        marketDataReadService.getFxRates().stream()
                .map(rate -> rate.symbol())
                .filter(Objects::nonNull)
                .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
                .filter(symbol -> !symbol.isBlank())
                .forEach(symbols::add);
        if (symbols.isEmpty()) {
            return;
        }
        long startedAt = System.nanoTime();
        Map<String, MarketPriceSummaryDto> refreshed = loadAndCacheSummaries(List.copyOf(symbols), clock.instant());
        log.info(
                "MARKET_DB_SUMMARY_CACHE_WARM symbolsRequested={} symbolsResolved={} durationMs={}",
                symbols.size(),
                refreshed.size(),
                nanosToMillis(startedAt));
    }

    private List<String> normalizeSymbols(List<String> symbols) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String raw : symbols) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            normalized.add(raw.trim().toUpperCase(Locale.ROOT));
        }
        return List.copyOf(normalized);
    }

    private Map<String, MarketPriceSummaryDto> loadAndCacheSummaries(List<String> symbols, Instant now) {
        Map<String, MarketPriceSummaryDto> computed = new LinkedHashMap<>();
        Instant expiresAt = now.plusMillis(Math.max(summaryCacheTtlMs, 1L));
        for (String symbol : symbols) {
            MarketPriceSummaryDto summary = buildPriceSummary(symbol, now);
            summaryCache.put(symbol, new SummaryCacheEntry(summary, expiresAt));
            if (summary != null) {
                computed.put(symbol, summary);
            }
        }
        return computed;
    }

    private MarketPriceSummaryDto buildPriceSummary(String symbol, Instant now) {
        Instant toExclusive = now.plus(1, ChronoUnit.DAYS);
        BigDecimal latestPrice;
        if (symbol.startsWith("FUND_")) {
            String fundCode = symbol.substring("FUND_".length());
            var latestOpt = fundNavHistoryRepository.findTopByFundCodeOrderByObservedAtDesc(fundCode);
            if (latestOpt.isEmpty() || latestOpt.get().getNav() == null) {
                return null;
            }
            latestPrice = latestOpt.get().getNav();
        } else if (usesFxRateHistory(symbol)) {
            List<HistoryPointDto> latestPoints =
                    fxRateHistoryRepository.findLatestHistoryPoint(symbol, PageRequest.of(0, 1));
            if (latestPoints.isEmpty() || latestPoints.get(0).value() == null) {
                return null;
            }
            latestPrice = latestPoints.get(0).value();
        } else {
            List<HistoryPointDto> latestPoints =
                    marketPriceHistoryRepository.findLatestHistoryPoint(symbol, PageRequest.of(0, 1));
            if (latestPoints.isEmpty() || latestPoints.get(0).value() == null) {
                return null;
            }
            latestPrice = latestPoints.get(0).value();
        }

        double change1D;
        double change1W;
        double change1M;
        double change3M;
        double change6M;
        double change1Y;
        if (symbol.startsWith("FUND_")) {
            String fundCode = symbol.substring("FUND_".length());
            change1D = computeFundNavTrailingPercent(fundCode, now, 1);
            change1W = computeFundNavTrailingPercent(fundCode, now, 7);
            change1M = computeFundNavTrailingPercent(fundCode, now, 30);
            change3M = computeFundNavTrailingPercent(fundCode, now, 90);
            change6M = computeFundNavTrailingPercent(fundCode, now, 180);
            change1Y = computeFundNavTrailingPercent(fundCode, now, 365);
        } else if (symbol.startsWith("TRBOND")) {
            // Bond ingest stores calendar-daily rows (EVDS daily + forward-fill). Prefer last day-to-day step;
            // fallback window if history is still warming up.
            change1D = computeTcmbBondLatestStepPercentChange(symbol, now, toExclusive);
            change1W = computePeriodChange(symbol, now.minus(7, ChronoUnit.DAYS), toExclusive);
            change1M = computePeriodChange(symbol, now.minus(45, ChronoUnit.DAYS), toExclusive);
            change3M = computePeriodChange(symbol, now.minus(120, ChronoUnit.DAYS), toExclusive);
            change6M = computePeriodChange(symbol, now.minus(210, ChronoUnit.DAYS), toExclusive);
            change1Y = computePeriodChange(symbol, now.minus(400, ChronoUnit.DAYS), toExclusive);
        } else {
            change1D = computeLatestDailyStepPercentChange(symbol);
            change1W = computePeriodChange(symbol, now.minus(7, ChronoUnit.DAYS), toExclusive);
            change1M = computePeriodChange(symbol, now.minus(30, ChronoUnit.DAYS), toExclusive);
            change3M = computePeriodChange(symbol, now.minus(90, ChronoUnit.DAYS), toExclusive);
            change6M = computePeriodChange(symbol, now.minus(180, ChronoUnit.DAYS), toExclusive);
            change1Y = computePeriodChange(symbol, now.minus(365, ChronoUnit.DAYS), toExclusive);
        }
        return new MarketPriceSummaryDto(latestPrice, change1D, change1W, change1M, change3M, change6M, change1Y);
    }

    private static long nanosToMillis(long startedAt) {
        return Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
    }

    private record SummaryCacheEntry(MarketPriceSummaryDto summary, Instant expiresAt) {}

    /**
     * Trailing return for daily TEFAS NAV: compare latest NAV (as of {@code now}) to the latest NAV
     * available as of {@code now - periodDays}. Avoids rolling windows that often contain fewer than two NAV prints.
     */
    private double computeFundNavTrailingPercent(String fundCode, Instant now, long periodDays) {
        Optional<FundNavHistoryEntry> latest =
                fundNavHistoryRepository.findTopByFundCodeAndObservedAtLessThanEqualOrderByObservedAtDesc(fundCode, now);
        if (latest.isEmpty() || latest.get().getNav() == null) {
            return 0d;
        }
        BigDecimal lastNav = latest.get().getNav();
        if (lastNav.compareTo(BigDecimal.ZERO) == 0) {
            return 0d;
        }
        Instant cutoff = now.minus(periodDays, ChronoUnit.DAYS);
        Optional<FundNavHistoryEntry> baseline =
                fundNavHistoryRepository.findTopByFundCodeAndObservedAtLessThanEqualOrderByObservedAtDesc(fundCode, cutoff);
        if (baseline.isEmpty() || baseline.get().getNav() == null) {
            return 0d;
        }
        if (Objects.equals(latest.get().getId(), baseline.get().getId())) {
            return 0d;
        }
        LocalDate latestDay = latest.get().getObservedAt().atZone(TURKEY).toLocalDate();
        LocalDate baselineDay = baseline.get().getObservedAt().atZone(TURKEY).toLocalDate();
        long spanDays = ChronoUnit.DAYS.between(baselineDay, latestDay);
        if (spanDays > periodDays + FUND_NAV_TRAILING_MAX_SLIP_DAYS) {
            return 0d;
        }
        BigDecimal firstNav = baseline.get().getNav();
        if (firstNav.compareTo(BigDecimal.ZERO) == 0) {
            return 0d;
        }
        return lastNav.subtract(firstNav)
                .divide(firstNav, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    /**
     * Percent move between the two most recent stored prints (TCMB bond yields are not intraday).
     */
    private double computeTcmbBondLatestStepPercentChange(String symbol, Instant now, Instant toExclusive) {
        List<DebugHistoryRowView> rows = marketPriceHistoryRepository.findLatestDebugRowsBySymbol(symbol, 2);
        if (rows != null && rows.size() >= 2) {
            BigDecimal last = rows.get(0).getPrice();
            BigDecimal prev = rows.get(1).getPrice();
            if (last != null
                    && prev != null
                    && prev.compareTo(BigDecimal.ZERO) != 0) {
                return last.subtract(prev)
                        .divide(prev, 8, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }
        }
        return computePeriodChange(symbol, now.minus(14, ChronoUnit.DAYS), toExclusive);
    }

    /**
     * Last calendar-day close vs previous calendar-day close. Avoids 0% on weekends when the live
     * scheduler keeps republishing Friday's close inside a rolling 24h window.
     */
    private double computeLatestDailyStepPercentChange(String symbol) {
        BigDecimal last;
        BigDecimal prev;
        if (usesFxRateHistory(symbol)) {
            var rows = fxRateHistoryRepository.findLastTwoDailyCloses(symbol);
            if (rows == null || rows.size() < 2) {
                Instant now = clock.instant();
                return computePeriodChange(symbol, now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS));
            }
            last = rows.get(0).getPrice();
            prev = rows.get(1).getPrice();
        } else {
            var rows = marketPriceHistoryRepository.findLastTwoDailyCloses(symbol);
            if (rows == null || rows.size() < 2) {
                Instant now = clock.instant();
                return computePeriodChange(symbol, now.minus(1, ChronoUnit.DAYS), now.plus(1, ChronoUnit.DAYS));
            }
            last = rows.get(0).getPrice();
            prev = rows.get(1).getPrice();
        }
        if (last == null || prev == null || prev.compareTo(BigDecimal.ZERO) == 0) {
            return 0d;
        }
        return last.subtract(prev)
                .divide(prev, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private double computePeriodChange(String symbol, Instant fromInclusive, Instant toExclusive) {
        List<HistoryPointDto> history = resolvePriceLikeHistory(symbol, fromInclusive, toExclusive);
        if (history.size() < 2) {
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

    private List<HistoryPointDto> resolvePriceLikeHistory(String symbol, Instant fromInclusive, Instant toExclusive) {
        if (symbol != null && symbol.toUpperCase(Locale.ROOT).startsWith("FUND_")) {
            String fundCode = symbol.substring("FUND_".length());
            return fundNavHistoryRepository.findHistoryPoints(fundCode, fromInclusive, toExclusive);
        }
        if (usesFxRateHistory(symbol)) {
            return fxRateHistoryRepository.findHistoryPoints(symbol, fromInclusive, toExclusive);
        }
        return marketPriceHistoryRepository.findHistoryPoints(symbol, fromInclusive, toExclusive);
    }

    /** TCMB crosses and TRY spot metals ({@code XAUTRY}, …) are stored in {@code mds_fx_rate_history}. */
    private static boolean usesFxRateHistory(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return false;
        }
        String wire = MarketCatalogSegmentRules.inferWireCategory(symbol);
        return MarketCatalogSegmentRules.usesFxHistoryPath(symbol, wire);
    }

    private static String normalizeFundCode(String fundCode) {
        String normalized = fundCode.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("FUND_")) {
            return normalized.substring("FUND_".length());
        }
        return normalized;
    }

    private static boolean shouldRepairFundNavGaps(List<HistoryPointDto> points, LocalDate from, LocalDate to) {
        if (points == null || points.isEmpty()) {
            return true;
        }
        if (points.size() < 2) {
            long spanDays = ChronoUnit.DAYS.between(from, to) + 1;
            return spanDays > 14;
        }
        for (int i = 1; i < points.size(); i++) {
            Instant prevAt = points.get(i - 1).time();
            Instant curAt = points.get(i).time();
            if (prevAt == null || curAt == null) {
                continue;
            }
            long gapDays = ChronoUnit.DAYS.between(
                    prevAt.atZone(ZoneOffset.UTC).toLocalDate(),
                    curAt.atZone(ZoneOffset.UTC).toLocalDate());
            if (gapDays > 10) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValid(String symbolOrCode, LocalDate from, LocalDate to) {
        if (symbolOrCode == null || symbolOrCode.isBlank() || from == null || to == null || to.isBefore(from)) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        String sym = symbolOrCode.trim().toUpperCase(Locale.ROOT);
        long maxAllowed = MAX_RANGE_DAYS;
        if (sym.startsWith("TRBOND") || sym.startsWith("TRGOVUSD")) {
            maxAllowed = MAX_TRBOND_RANGE_DAYS;
        }
        return days <= maxAllowed;
    }
}
