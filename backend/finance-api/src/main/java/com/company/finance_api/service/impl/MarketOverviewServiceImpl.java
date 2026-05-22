package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.market.MarketOverviewCategoryRules;
import com.company.finance_api.dto.MarketInsightsResponse;
import com.company.finance_api.dto.MarketOverviewItemResponse;
import com.company.finance_api.dto.MarketOverviewPageResponse;
import com.company.finance_api.repository.InstrumentPriceRepository;
import com.company.finance_api.service.CurrencyConversionService;
import com.company.finance_api.service.InstrumentService;
import com.company.finance_api.service.MarketOverviewService;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MarketOverviewServiceImpl implements MarketOverviewService {

    private static final Logger log = LoggerFactory.getLogger(MarketOverviewServiceImpl.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(5);
    private static final int MAX_OVERVIEW_PAGE_SIZE = 50;
    private static final String INSIGHTS_CACHE_KEY = "market:insights";
    private static final String USD = "USD";
    private static final String TRY = "TRY";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final InstrumentService instrumentService;
    private final InstrumentPriceRepository instrumentPriceRepository;
    private final CurrencyConversionService currencyConversionService;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;

    @Value("${clients.market-data.base-url:http://market-data-service:8080}")
    private String marketDataBaseUrl;

    @Value("${clients.analytics.base-url:http://analytics-service:8080}")
    private String analyticsBaseUrl;

    private final RestClient restClient = RestClient.create();

    public MarketOverviewServiceImpl(
            InstrumentService instrumentService,
            InstrumentPriceRepository instrumentPriceRepository,
            CurrencyConversionService currencyConversionService,
            ObjectMapper objectMapper,
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider
    ) {
        this.instrumentService = instrumentService;
        this.instrumentPriceRepository = instrumentPriceRepository;
        this.currencyConversionService = currencyConversionService;
        this.objectMapper = objectMapper;
        this.stringRedisTemplateProvider = stringRedisTemplateProvider;
    }

    @Override
    public MarketOverviewPageResponse getOverview(int page, int size, String category, String search, String targetCurrency, String sort) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.min(Math.max(size, 1), MAX_OVERVIEW_PAGE_SIZE);
        String normalizedCategory = normalize(category);
        String normalizedSearch = normalize(search);
        String normalizedCurrency = currencyConversionService.normalizeCurrency(targetCurrency);
        String normalizedSort = normalize(sort);
        String mdsSegment = MarketOverviewCategoryRules.toMdsSegment(normalizedCategory);

        String cacheKey = cacheKey(resolvedPage, resolvedSize, normalizedCategory, normalizedSearch, normalizedCurrency, normalizedSort);
        Optional<MarketOverviewPageResponse> cached = readFromCache(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        List<MarketBaseItem> merged = loadMergedBaseItems(mdsSegment, normalizedCategory).stream()
                .filter(item -> searchMatches(item, normalizedSearch))
                .filter(item -> MarketOverviewCategoryRules.matchesUiCategory(
                        item.symbol(),
                        item.source(),
                        normalizedCategory
                ))
                .toList();

        Map<String, BigDecimal> pricesForChanges = merged.stream()
                .collect(Collectors.toMap(
                        MarketBaseItem::symbol,
                        MarketBaseItem::price,
                        (left, right) -> left
                ));
        // Overview table: MDS summary is authoritative for horizon %; skip 5× DB baseline scans per request.
        Map<String, HistoricalChanges> changesBySymbol = enrichHistoricalChangesWithMdsSummary(
                Map.of(),
                new ArrayList<>(pricesForChanges.keySet())
        );
        List<String> allSymbols = merged.stream().map(MarketBaseItem::symbol).toList();
        Map<String, TrendEnrichment> contextualTrends = computeContextualTrendEnrichments(allSymbols, changesBySymbol);

        SortDirective sortDirective = SortDirective.parse(normalizedSort);
        List<MarketBaseItem> sorted = new ArrayList<>(merged);
        sorted.sort(baseItemComparator(sortDirective, changesBySymbol, normalizedCurrency, contextualTrends));

        int totalElements = sorted.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / resolvedSize);
        int start = resolvedPage * resolvedSize;
        int end = Math.min(start + resolvedSize, totalElements);
        if (start >= totalElements) {
            MarketOverviewPageResponse emptyPage = new MarketOverviewPageResponse(List.of(), resolvedPage, resolvedSize, totalElements, totalPages);
            writeToCache(cacheKey, emptyPage);
            return emptyPage;
        }

        List<MarketBaseItem> currentPage = sorted.subList(start, end);

        List<CompletableFuture<MarketOverviewItemResponse>> futures = currentPage.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> enrichOverviewRow(
                        item,
                        normalizedCurrency,
                        changesBySymbol.getOrDefault(item.symbol(), HistoricalChanges.empty()),
                        contextualTrends.getOrDefault(item.symbol(), TrendEnrichment.empty())
                )))
                .toList();

        List<MarketOverviewItemResponse> content = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        MarketOverviewPageResponse response = new MarketOverviewPageResponse(
                content,
                resolvedPage,
                resolvedSize,
                totalElements,
                totalPages
        );
        writeToCache(cacheKey, response);
        return response;
    }

    @Override
    public MarketInsightsResponse getInsights(String targetCurrency) {
        String normalizedCurrency = currencyConversionService.normalizeCurrency(targetCurrency);
        String currencyInsightsCacheKey = INSIGHTS_CACHE_KEY + ":currency:" + normalizedCurrency;
        Optional<MarketInsightsResponse> cached = readFromCache(currencyInsightsCacheKey, new TypeReference<>() {
        });
        if (cached.isPresent()) {
            return cached.get();
        }

        List<MarketBaseItem> baseItems = loadMergedBaseItems();
        Map<String, BigDecimal> currentPricesBySymbol = baseItems.stream()
                .collect(Collectors.toMap(
                        MarketBaseItem::symbol,
                        MarketBaseItem::price,
                        (left, right) -> left
                ));
        Map<String, HistoricalChanges> changesBySymbol = enrichHistoricalChangesWithMdsSummary(
                fetchHistoricalChanges(currentPricesBySymbol),
                new ArrayList<>(currentPricesBySymbol.keySet())
        );
        List<String> allSymbols = baseItems.stream().map(MarketBaseItem::symbol).toList();
        Map<String, TrendEnrichment> contextualTrends = computeContextualTrendEnrichments(allSymbols, changesBySymbol);
        List<MarketOverviewItemResponse> all = enrichAll(baseItems, normalizedCurrency, changesBySymbol, contextualTrends);
        all = applySummaryChangeFallback(all);
        List<MarketOverviewItemResponse> changeReady = all.stream()
                .filter(item -> item.change24h() != null)
                .toList();

        List<MarketOverviewItemResponse> topGainers = changeReady.stream()
                .sorted(Comparator.comparing(MarketOverviewItemResponse::change24h).reversed())
                .limit(5)
                .toList();

        List<MarketOverviewItemResponse> topLosers = changeReady.stream()
                .sorted(Comparator.comparing(MarketOverviewItemResponse::change24h))
                .limit(5)
                .toList();

        MarketInsightsResponse response = new MarketInsightsResponse(topGainers, topLosers);
        writeToCache(currencyInsightsCacheKey, response);
        return response;
    }

    private List<MarketOverviewItemResponse> enrichAll(
            List<MarketBaseItem> baseItems,
            String targetCurrency,
            Map<String, HistoricalChanges> changesBySymbol,
            Map<String, TrendEnrichment> contextualTrends
    ) {
        List<CompletableFuture<MarketOverviewItemResponse>> futures = baseItems.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> enrichWithAnalytics(
                item,
                targetCurrency,
                changesBySymbol.getOrDefault(item.symbol(), HistoricalChanges.empty()),
                contextualTrends.getOrDefault(item.symbol(), TrendEnrichment.empty())
        )))
                .toList();
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private List<MarketBaseItem> loadMergedBaseItems() {
        return loadMergedBaseItems(null, null);
    }

    private List<MarketBaseItem> loadMergedBaseItems(String mdsSegment, String normalizedCategory) {
        List<MarketPriceDto> prices = fetchLatestPrices(mdsSegment);
        // TCMB crosses + Stooq spot metals are published on MDS /api/market/fx, not always in /prices snapshot.
        if ("forex".equalsIgnoreCase(mdsSegment) || "metals".equalsIgnoreCase(mdsSegment)) {
            prices = mergeDistinctPrices(prices, fetchFxRatesAsPrices());
        }
        Map<String, Instrument> instrumentsBySymbol = instrumentService.getAllActive()
                .stream()
                .collect(Collectors.toMap(
                        instrument -> instrument.getSymbol().trim().toUpperCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left
                ));
        List<MarketBaseItem> filtered = prices.stream()
                .filter(price -> price.symbol() != null && !price.symbol().isBlank())
                .map(price -> {
                    String symbol = price.symbol().trim().toUpperCase(Locale.ROOT);
                    Instrument instrument = instrumentsBySymbol.get(symbol);
                    return mergeBase(price, instrument);
                })
                .toList();
        if (!filtered.isEmpty()) {
            return filtered;
        }
        if (StringUtils.hasText(mdsSegment)) {
            log.warn("Market overview empty for segment={} category={}", mdsSegment, normalizedCategory);
            return List.of();
        }
        log.warn("Market overview empty, returning fallback minimal dataset");
        return fallbackMinimalItems(prices, instrumentsBySymbol);
    }

    private static List<MarketPriceDto> mergeDistinctPrices(List<MarketPriceDto> primary, List<MarketPriceDto> extra) {
        LinkedHashMap<String, MarketPriceDto> bySymbol = new LinkedHashMap<>();
        for (MarketPriceDto p : primary) {
            if (p != null && StringUtils.hasText(p.symbol())) {
                bySymbol.put(p.symbol().trim().toUpperCase(Locale.ROOT), p);
            }
        }
        for (MarketPriceDto p : extra) {
            if (p != null && StringUtils.hasText(p.symbol())) {
                bySymbol.putIfAbsent(p.symbol().trim().toUpperCase(Locale.ROOT), p);
            }
        }
        return new ArrayList<>(bySymbol.values());
    }

    private List<MarketPriceDto> fetchFxRatesAsPrices() {
        String url = UriComponentsBuilder.fromHttpUrl(marketDataBaseUrl)
                .path("/api/market/fx")
                .toUriString();
        try {
            List<FxRateWire> body = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (body == null || body.isEmpty()) {
                return List.of();
            }
            List<MarketPriceDto> out = new ArrayList<>(body.size());
            for (FxRateWire fx : body) {
                if (fx == null || !StringUtils.hasText(fx.symbol())) {
                    continue;
                }
                BigDecimal mid = fx.mid();
                if (mid == null) {
                    mid = fx.ask() != null ? fx.ask() : fx.bid();
                }
                if (mid == null) {
                    continue;
                }
                String canonical = fx.symbol().trim().toUpperCase(Locale.ROOT);
                mid = CurrencyConversionServiceImpl.normalizeFxMidForTryHub(canonical, mid);
                if (mid == null) {
                    continue;
                }
                out.add(new MarketPriceDto(
                        canonical,
                        mid,
                        StringUtils.hasText(fx.source()) ? fx.source() : "TCMB",
                        fx.timestamp(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));
            }
            return out;
        } catch (Exception ex) {
            log.warn("MARKET_OVERVIEW_FX_FETCH_FAILED reason={}", ex.toString());
            return List.of();
        }
    }

    private MarketBaseItem mergeBase(MarketPriceDto price, Instrument instrument) {
        String wireCategory = MarketOverviewCategoryRules.inferWireCategory(price.symbol());
        String name = instrument == null ? price.symbol() : instrument.getName();
        Long instrumentId = instrument == null ? null : instrument.getId();
        return new MarketBaseItem(
                price.symbol(),
                name,
                price.price(),
                wireCategory,
                instrumentId,
                price.source(),
                price.volume24h(),
                price.openInterest(),
                price.dayOpen(),
                price.dayHigh(),
                price.dayLow(),
                price.exchangeName(),
                price.underlyingSymbol(),
                price.contractExpiry(),
                price.linkedSpotSymbol(),
                price.spotSpreadPct(),
                price.spotSpreadAbs());
    }

    private List<MarketBaseItem> fallbackMinimalItems(
            List<MarketPriceDto> prices,
            Map<String, Instrument> instrumentsBySymbol
    ) {
        List<MarketBaseItem> fromPriceSymbols = prices.stream()
                .filter(price -> price.symbol() != null && !price.symbol().isBlank())
                .map(price -> {
                    String symbol = price.symbol().trim().toUpperCase(Locale.ROOT);
                    Instrument instrument = instrumentsBySymbol.get(symbol);
                    MarketPriceDto wire = new MarketPriceDto(
                            symbol,
                            BigDecimal.ZERO,
                            price.source(),
                            price.timestamp(),
                            price.volume24h(),
                            price.openInterest(),
                            price.dayOpen(),
                            price.dayHigh(),
                            price.dayLow(),
                            price.exchangeName(),
                            price.underlyingSymbol(),
                            price.contractExpiry(),
                            price.linkedSpotSymbol(),
                            price.spotSpreadPct(),
                            price.spotSpreadAbs());
                    return mergeBase(wire, instrument);
                })
                .distinct()
                .toList();
        if (!fromPriceSymbols.isEmpty()) {
            return fromPriceSymbols;
        }
        return instrumentService.getAllActive().stream()
                .map(instrument -> mergeBase(
                        new MarketPriceDto(
                                instrument.getSymbol(),
                                BigDecimal.ZERO,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null),
                        instrument))
                .toList();
    }

    private boolean searchMatches(MarketBaseItem item, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }
        String lowered = search.toLowerCase(Locale.ROOT);
        return item.symbol().toLowerCase(Locale.ROOT).contains(lowered)
                || item.name().toLowerCase(Locale.ROOT).contains(lowered);
    }

    private Comparator<MarketBaseItem> baseItemComparator(
            SortDirective sort,
            Map<String, HistoricalChanges> changes,
            String targetCurrency,
            Map<String, TrendEnrichment> contextualTrends
    ) {
        Comparator<MarketBaseItem> primary = switch (sort.field()) {
            case "price" -> Comparator.comparing(
                    MarketBaseItem::price,
                    (a, b) -> cmpNullableBigDecimal(a, b, sort.desc()));
            case "displayamount" -> Comparator.comparing(
                    item -> convertDisplayPrice(item.price(), item.symbol(), targetCurrency),
                    (a, b) -> cmpNullableBigDecimal(a, b, sort.desc()));
            case "change1m" -> Comparator.comparing(
                    item -> lookupChange(changes, item.symbol()).change1M(),
                    (a, b) -> cmpNullableBigDecimal(a, b, sort.desc()));
            case "change3m" -> Comparator.comparing(
                    item -> lookupChange(changes, item.symbol()).change3M(),
                    (a, b) -> cmpNullableBigDecimal(a, b, sort.desc()));
            case "change6m" -> Comparator.comparing(
                    item -> lookupChange(changes, item.symbol()).change6M(),
                    (a, b) -> cmpNullableBigDecimal(a, b, sort.desc()));
            case "change1y" -> Comparator.comparing(
                    item -> lookupChange(changes, item.symbol()).change1Y(),
                    (a, b) -> cmpNullableBigDecimal(a, b, sort.desc()));
            case "trendscore" -> Comparator.comparing(
                    item -> {
                        TrendEnrichment trend = contextualTrends.get(item.symbol());
                        return trend != null ? trend.score() : null;
                    },
                    (a, b) -> cmpNullableBigDecimal(a, b, sort.desc()));
            case "symbol" -> sort.desc()
                    ? Comparator.comparing(MarketBaseItem::symbol, String.CASE_INSENSITIVE_ORDER.reversed())
                    : Comparator.comparing(MarketBaseItem::symbol, String.CASE_INSENSITIVE_ORDER);
            case "change1d", "change24h" -> Comparator.comparing(
                    item -> lookupChange(changes, item.symbol()).change1D(),
                    (a, b) -> cmpNullableBigDecimal(a, b, sort.desc()));
            default -> Comparator.comparing(
                    item -> lookupChange(changes, item.symbol()).change1D(),
                    (a, b) -> cmpNullableBigDecimal(a, b, sort.desc()));
        };
        return primary.thenComparing(MarketBaseItem::symbol, String.CASE_INSENSITIVE_ORDER);
    }

    private static HistoricalChanges lookupChange(Map<String, HistoricalChanges> changes, String symbol) {
        return changes.getOrDefault(symbol, HistoricalChanges.empty());
    }

    private static int cmpNullableBigDecimal(BigDecimal a, BigDecimal b, boolean desc) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        int c = a.compareTo(b);
        return desc ? -c : c;
    }

    private TrendEnrichment resolveTrendEnrichment(String symbol) {
        try {
            List<TrendMetricWire> metrics = fetchTrendMetrics(symbol);
            if (metrics == null || metrics.isEmpty()) {
                return TrendEnrichment.empty();
            }
            TrendMetricWire last = metrics.get(metrics.size() - 1);
            String dir = last.trendDirection == null ? "NEUTRAL" : last.trendDirection.trim().toUpperCase(Locale.ROOT);
            String label = switch (dir) {
                case "BULLISH" -> "STRONG";
                case "BEARISH" -> "WEAK";
                default -> "NEUTRAL";
            };
            String refined = label;
            if ("BULLISH".equals(dir) && last.momentum != null && last.momentum.compareTo(new BigDecimal("2.5")) > 0) {
                refined = "VERY_STRONG";
            }
            BigDecimal score = trendScoreFromMomentum(last.momentum);
            if (score == null) {
                return TrendEnrichment.empty();
            }
            return new TrendEnrichment(score, refined);
        } catch (Exception ex) {
            log.debug("TREND_METRIC_SKIP symbol={} reason={}", symbol, ex.toString());
            return TrendEnrichment.empty();
        }
    }

    private TrendEnrichment resolveTrendForItem(String symbol, TrendEnrichment contextualTrend) {
        TrendEnrichment analyticsTrend = resolveTrendEnrichment(symbol);
        if (analyticsTrend.score() != null) {
            return analyticsTrend;
        }
        if (contextualTrend != null && contextualTrend.score() != null) {
            return contextualTrend;
        }
        return TrendEnrichment.empty();
    }

    /**
     * Relative trend score (0–100) from horizon % moves, compared across the current market universe.
     * Used when analytics trend metrics are not yet materialized for a symbol.
     */
    private Map<String, TrendEnrichment> computeContextualTrendEnrichments(
            List<String> symbols,
            Map<String, HistoricalChanges> changesBySymbol
    ) {
        if (symbols == null || symbols.isEmpty()) {
            return Map.of();
        }
        int n = symbols.size();
        double[] signals = new double[n];
        for (int i = 0; i < n; i++) {
            signals[i] = weeklyTrendSignal(changesBySymbol.getOrDefault(symbols.get(i), HistoricalChanges.empty()));
        }
        double med = median(signals);
        double mean = Arrays.stream(signals).average().orElse(0);
        double sigma = Math.max(stddev(signals, mean), 0.0001);
        double dispersion = Math.max(median(Arrays.stream(signals).map(v -> Math.abs(v - med)).toArray()), 0.0001);
        double breadth = (double) Arrays.stream(signals).filter(v -> v > 0).count() / n;
        double[] sortedSignals = Arrays.copyOf(signals, n);
        Arrays.sort(sortedSignals);

        Map<String, TrendEnrichment> out = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            String symbol = symbols.get(i);
            double signal = signals[i];
            int belowOrEqual = 0;
            for (double sortedSignal : sortedSignals) {
                if (sortedSignal <= signal) {
                    belowOrEqual++;
                }
            }
            double percentile = (belowOrEqual / (double) n) * 100.0;
            double relativeWeekly = signal - med;
            double z = (signal - mean) / sigma;
            double percentileScore = percentile;
            double relativeScore = clamp(50 + (relativeWeekly / dispersion) * 20, 0, 100);
            double anomalyScore = clamp(50 + z * 10, 0, 100);
            double breadthAdjust = signal >= 0 ? (0.5 - breadth) * 15 : -(0.5 - breadth) * 15;
            double trendScore = clamp(
                    percentileScore * 0.5 + relativeScore * 0.3 + anomalyScore * 0.2 + breadthAdjust,
                    0,
                    100
            );
            BigDecimal scoreBd = BigDecimal.valueOf(trendScore).setScale(1, RoundingMode.HALF_UP);
            out.put(symbol, new TrendEnrichment(scoreBd, labelForTrendScore(trendScore)));
        }
        return out;
    }

    private static double weeklyTrendSignal(HistoricalChanges changes) {
        if (changes == null) {
            return 0;
        }
        if (changes.change1M() != null) {
            return changes.change1M().doubleValue() / 4.0;
        }
        if (changes.change1D() != null) {
            return changes.change1D().doubleValue();
        }
        return 0;
    }

    private static String labelForTrendScore(double score) {
        if (score < 35) {
            return "WEAK";
        }
        if (score < 65) {
            return "NEUTRAL";
        }
        if (score < 85) {
            return "STRONG";
        }
        return "VERY_STRONG";
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double median(double[] values) {
        if (values.length == 0) {
            return 0;
        }
        double[] sorted = Arrays.copyOf(values, values.length);
        Arrays.sort(sorted);
        int mid = sorted.length / 2;
        if (sorted.length % 2 == 0) {
            return (sorted[mid - 1] + sorted[mid]) / 2.0;
        }
        return sorted[mid];
    }

    private static double stddev(double[] values, double mean) {
        if (values.length == 0) {
            return 0;
        }
        double variance = 0;
        for (double value : values) {
            variance += (value - mean) * (value - mean);
        }
        variance /= values.length;
        return Math.sqrt(variance);
    }

    private List<TrendMetricWire> fetchTrendMetrics(String symbol) {
        String url = UriComponentsBuilder.fromHttpUrl(analyticsBaseUrl)
                .path("/api/analytics/instruments/{symbol}/trend")
                .buildAndExpand(symbol)
                .toUriString();
        AnalyticsApiResponse<List<TrendMetricWire>> body = restClient.get()
                .uri(url)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (body == null || body.data() == null) {
            return List.of();
        }
        return body.data();
    }

    private static BigDecimal trendScoreFromMomentum(BigDecimal momentum) {
        if (momentum == null) {
            return null;
        }
        double m = momentum.doubleValue();
        double raw = 50d + m * 20d;
        double clamped = Math.max(0d, Math.min(100d, raw));
        return BigDecimal.valueOf(clamped).setScale(1, RoundingMode.HALF_UP);
    }

    private record SortDirective(String field, boolean desc) {
        static SortDirective parse(String normalizedSort) {
            if (!StringUtils.hasText(normalizedSort)) {
                return new SortDirective("change1d", true);
            }
            String[] parts = normalizedSort.split(",", 2);
            String raw = parts[0].trim().toLowerCase(Locale.ROOT);
            if ("change24h".equals(raw)) {
                raw = "change1d";
            }
            boolean desc = parts.length < 2 || !"asc".equalsIgnoreCase(parts[1].trim());
            return new SortDirective(raw, desc);
        }
    }

    private record TrendEnrichment(BigDecimal score, String label) {
        static TrendEnrichment empty() {
            return new TrendEnrichment(null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class TrendMetricWire {
        public String trendDirection;
        public BigDecimal momentum;
        public BigDecimal priceSlope;
    }

    /**
     * Fast path for paginated overview: no per-symbol analytics HTTP (candles/trend metrics).
     * Uses MDS horizon % and universe-relative contextual trend scores already computed for sorting.
     */
    private MarketOverviewItemResponse enrichOverviewRow(
            MarketBaseItem base,
            String targetCurrency,
            HistoricalChanges historicalChanges,
            TrendEnrichment contextualTrend
    ) {
        TrendEnrichment trend = contextualTrend != null && contextualTrend.score() != null
                ? contextualTrend
                : TrendEnrichment.empty();
        BigDecimal nativePx = base.price();
        BigDecimal convertedPrice = applyPrecision(convertDisplayPrice(nativePx, base.symbol(), targetCurrency), base.category());
        BigDecimal convertedHigh = applyPrecision(convertDisplayPrice(base.dayHigh(), base.symbol(), targetCurrency), base.category());
        BigDecimal convertedLow = applyPrecision(convertDisplayPrice(base.dayLow(), base.symbol(), targetCurrency), base.category());
        BigDecimal change24h = historicalChanges.change1D();
        return mapToOverviewItem(
                base,
                nativePx,
                convertedPrice,
                change24h,
                historicalChanges,
                convertedHigh,
                convertedLow,
                trend);
    }

    private MarketOverviewItemResponse enrichWithAnalytics(
            MarketBaseItem base,
            String targetCurrency,
            HistoricalChanges historicalChanges,
            TrendEnrichment contextualTrend
    ) {
        try {
            List<AnalyticsCandleDto> candles = fetchCandles(base.symbol());
            AnalyticsMetrics metrics = computeMetrics(base.price(), candles);
            TrendEnrichment trend = resolveTrendForItem(base.symbol(), contextualTrend);
            BigDecimal nativePx = base.price();
            BigDecimal convertedPrice = applyPrecision(convertDisplayPrice(nativePx, base.symbol(), targetCurrency), base.category());
            BigDecimal convertedHigh = applyPrecision(convertDisplayPrice(metrics.high24h(), base.symbol(), targetCurrency), base.category());
            BigDecimal convertedLow = applyPrecision(convertDisplayPrice(metrics.low24h(), base.symbol(), targetCurrency), base.category());
            BigDecimal change24h = metrics.change24h() == null ? historicalChanges.change1D() : metrics.change24h();
            return mapToOverviewItem(
                    base,
                    nativePx,
                    convertedPrice,
                    change24h,
                    historicalChanges,
                    convertedHigh,
                    convertedLow,
                    trend);
        } catch (Exception ex) {
            log.warn("MARKET_OVERVIEW_ANALYTICS_FALLBACK symbol={} reason={}", base.symbol(), ex.toString());
            BigDecimal nativePx = base.price();
            BigDecimal convertedPrice = applyPrecision(convertDisplayPrice(nativePx, base.symbol(), targetCurrency), base.category());
            TrendEnrichment trend = resolveTrendForItem(base.symbol(), contextualTrend);
            return mapToOverviewItem(
                    base,
                    nativePx,
                    convertedPrice,
                    historicalChanges.change1D(),
                    historicalChanges,
                    null,
                    null,
                    trend);
        }
    }

    private MarketOverviewItemResponse mapToOverviewItem(
            MarketBaseItem base,
            BigDecimal nativePx,
            BigDecimal convertedPrice,
            BigDecimal change24h,
            HistoricalChanges historicalChanges,
            BigDecimal high24h,
            BigDecimal low24h,
            TrendEnrichment trend
    ) {
        return new MarketOverviewItemResponse(
                base.symbol(),
                base.name(),
                nativePx,
                convertedPrice,
                change24h,
                historicalChanges.change1D(),
                historicalChanges.change1M(),
                historicalChanges.change3M(),
                historicalChanges.change6M(),
                historicalChanges.change1Y(),
                high24h,
                low24h,
                base.category(),
                base.instrumentId(),
                trend.score(),
                trend.label(),
                base.volume24h(),
                base.openInterest(),
                base.dayOpen(),
                base.dayHigh(),
                base.dayLow(),
                base.exchangeName(),
                base.underlyingSymbol(),
                base.contractExpiry(),
                base.linkedSpotSymbol(),
                base.spotSpreadPct(),
                base.spotSpreadAbs());
    }

    private AnalyticsMetrics computeMetrics(BigDecimal latestPrice, List<AnalyticsCandleDto> candles) {
        if (candles == null || candles.isEmpty()) {
            return AnalyticsMetrics.empty();
        }

        List<AnalyticsCandleDto> sorted = new ArrayList<>(candles);
        sorted.sort(Comparator.comparing(AnalyticsCandleDto::timeAnchor));
        AnalyticsCandleDto latest = sorted.get(sorted.size() - 1);
        BigDecimal previousClose = sorted.size() >= 2 ? sorted.get(sorted.size() - 2).close() : null;

        BigDecimal change24h = null;
        if (previousClose != null && previousClose.compareTo(BigDecimal.ZERO) != 0) {
            change24h = latestPrice
                    .subtract(previousClose)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousClose, 4, RoundingMode.HALF_UP);
        }

        return new AnalyticsMetrics(change24h, latest.high(), latest.low());
    }

    private List<MarketPriceDto> fetchLatestPrices(String mdsSegment) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(marketDataBaseUrl)
                .path("/api/market/prices");
        if (StringUtils.hasText(mdsSegment)) {
            builder.queryParam("segment", mdsSegment);
        }
        String url = builder.toUriString();
        List<MarketPriceDto> body = restClient.get()
                .uri(url)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        return body == null ? List.of() : body;
    }

    private List<AnalyticsCandleDto> fetchCandles(String symbol) {
        String url = UriComponentsBuilder.fromHttpUrl(analyticsBaseUrl)
                .path("/api/analytics/instruments/{symbol}/candles")
                .queryParam("from", LocalDate.now().minusDays(2))
                .queryParam("to", LocalDate.now())
                .buildAndExpand(symbol)
                .toUriString();

        AnalyticsApiResponse<List<AnalyticsCandleDto>> body = restClient.get()
                .uri(url)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (body == null || body.data() == null) {
            return List.of();
        }
        return body.data();
    }

    private List<MarketOverviewItemResponse> applySummaryChangeFallback(List<MarketOverviewItemResponse> items) {
        List<String> symbolsNeedingFallback = items.stream()
                .filter(item -> item.change24h() == null)
                .map(MarketOverviewItemResponse::symbol)
                .distinct()
                .toList();
        if (symbolsNeedingFallback.isEmpty()) {
            return items;
        }
        Map<String, SummaryDto> summaryBySymbol = fetchPriceSummary(symbolsNeedingFallback);
        if (summaryBySymbol.isEmpty()) {
            return items;
        }
        return items.stream()
                .map(item -> {
                    if (item.change24h() != null) {
                        return item;
                    }
                    SummaryDto summary = summaryBySymbol.get(item.symbol());
                    if (summary == null || summary.change1D() == null) {
                        return item;
                    }
                    return new MarketOverviewItemResponse(
                            item.symbol(),
                            item.name(),
                            item.nativePrice(),
                            item.price(),
                            summary.change1D(),
                            item.change1D(),
                            item.change1M(),
                            item.change3M(),
                            item.change6M(),
                            item.change1Y(),
                            item.high24h(),
                            item.low24h(),
                            item.category(),
                            item.instrumentId(),
                            item.trendScore(),
                            item.trendLabel(),
                            item.volume24h(),
                            item.openInterest(),
                            item.dayOpen(),
                            item.dayHigh(),
                            item.dayLow(),
                            item.exchangeName(),
                            item.underlyingSymbol(),
                            item.contractExpiry(),
                            item.linkedSpotSymbol(),
                            item.spotSpreadPct(),
                            item.spotSpreadAbs());
                })
                .toList();
    }

    /**
     * Fills horizon % moves from market-data-service when {@code instrument_prices} has no usable baselines
     * (common for Kafka-fed tape while MDS {@code mds_market_price_history} is authoritative).
     */
    private Map<String, HistoricalChanges> enrichHistoricalChangesWithMdsSummary(
            Map<String, HistoricalChanges> fromInstrumentDb,
            List<String> symbols
    ) {
        if (symbols.isEmpty()) {
            return fromInstrumentDb;
        }
        Map<String, HistoricalChanges> out = new LinkedHashMap<>();
        final int chunkSize = 100;
        for (int i = 0; i < symbols.size(); i += chunkSize) {
            List<String> chunk = symbols.subList(i, Math.min(symbols.size(), i + chunkSize));
            Map<String, SummaryDto> mdsBySymbol = fetchPriceSummary(chunk);
            for (String sym : chunk) {
                HistoricalChanges db = fromInstrumentDb.getOrDefault(sym, HistoricalChanges.empty());
                SummaryDto mds = mdsBySymbol.get(sym);
                out.put(sym, mergeHistoricalWithSummary(db, mds));
            }
        }
        return out;
    }

    private static HistoricalChanges mergeHistoricalWithSummary(HistoricalChanges db, SummaryDto mds) {
        if (mds == null) {
            return db;
        }
        return new HistoricalChanges(
                coalescePct(db.change1D(), mds.change1D()),
                coalescePct(db.change1M(), mds.change1M()),
                coalescePct(db.change3M(), mds.change3M()),
                coalescePct(db.change6M(), mds.change6M()),
                coalescePct(db.change1Y(), mds.change1Y())
        );
    }

    private static BigDecimal coalescePct(BigDecimal preferred, BigDecimal fallback) {
        if (preferred == null) {
            return fallback;
        }
        // instrument_prices baselines can be missing for Yahoo futures while MDS history has horizons
        if (preferred.compareTo(BigDecimal.ZERO) == 0
                && fallback != null
                && fallback.compareTo(BigDecimal.ZERO) != 0) {
            return fallback;
        }
        return preferred;
    }

    private Map<String, HistoricalChanges> fetchHistoricalChanges(Map<String, BigDecimal> currentPricesBySymbol) {
        if (currentPricesBySymbol == null || currentPricesBySymbol.isEmpty()) {
            return Map.of();
        }
        List<String> symbols = currentPricesBySymbol.keySet().stream().toList();
        Instant now = Instant.now();
        Map<String, BigDecimal> change1DBase = fetchBaselinePrices(symbols, now.minus(Duration.ofDays(1)));
        Map<String, BigDecimal> change1MBase = fetchBaselinePrices(symbols, now.minus(Duration.ofDays(30)));
        Map<String, BigDecimal> change3MBase = fetchBaselinePrices(symbols, now.minus(Duration.ofDays(90)));
        Map<String, BigDecimal> change6MBase = fetchBaselinePrices(symbols, now.minus(Duration.ofDays(180)));
        Map<String, BigDecimal> change1YBase = fetchBaselinePrices(symbols, now.minus(Duration.ofDays(365)));

        return symbols.stream()
                .distinct()
                .collect(Collectors.toMap(
                        Function.identity(),
                        symbol -> {
                            BigDecimal current = currentPricesBySymbol.get(symbol);
                            return new HistoricalChanges(
                                    computePercentageChange(current, change1DBase.get(symbol)),
                                    computePercentageChange(current, change1MBase.get(symbol)),
                                    computePercentageChange(current, change3MBase.get(symbol)),
                                    computePercentageChange(current, change6MBase.get(symbol)),
                                    computePercentageChange(current, change1YBase.get(symbol))
                            );
                        }
                ));
    }

    private Map<String, BigDecimal> fetchBaselinePrices(List<String> symbols, Instant target) {
        return instrumentPriceRepository.findLatestPricesAtOrBefore(symbols, PriceType.MARKET.name(), target)
                .stream()
                .collect(Collectors.toMap(
                        InstrumentPriceRepository.SymbolPriceView::getSymbol,
                        InstrumentPriceRepository.SymbolPriceView::getPrice,
                        (left, right) -> left
                ));
    }

    private BigDecimal computePercentageChange(BigDecimal current, BigDecimal old) {
        if (current == null || old == null || old.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(old)
                .divide(old, 8, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private Map<String, SummaryDto> fetchPriceSummary(List<String> symbols) {
        if (symbols.isEmpty()) {
            return Map.of();
        }
        // Yahoo futures symbols contain '='; must use encoded URI — raw toUriString() + RestClient
        // drops or mis-parses GC=F,SI=F so MDS summary never merges into overview 1M–1Y.
        URI uri = UriComponentsBuilder.fromHttpUrl(marketDataBaseUrl)
                .path("/api/market/prices/summary")
                .queryParam("symbols", String.join(",", symbols))
                .encode()
                .build()
                .toUri();
        try {
            String payload = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            return parsePriceSummaryPayload(payload, symbols.size());
        } catch (Exception ex) {
            log.warn("MARKET_SUMMARY_FALLBACK_FAILED symbols={} reason={}", symbols.size(), ex.toString());
            return Map.of();
        }
    }

    private Map<String, SummaryDto> parsePriceSummaryPayload(String payload, int symbolsRequested) {
        if (!StringUtils.hasText(payload)) {
            return Map.of();
        }
        try {
            Map<String, Map<String, Object>> raw = objectMapper.readValue(
                    payload,
                    new TypeReference<Map<String, Map<String, Object>>>() {}
            );
            if (raw == null || raw.isEmpty()) {
                return Map.of();
            }
            Map<String, SummaryDto> out = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Object>> entry : raw.entrySet()) {
                Map<String, Object> row = entry.getValue();
                if (row == null) {
                    continue;
                }
                out.put(entry.getKey(), new SummaryDto(
                        jsonNumberToBigDecimal(row.get("price")),
                        jsonNumberToBigDecimal(row.get("change1D")),
                        jsonNumberToBigDecimal(row.get("change1M")),
                        jsonNumberToBigDecimal(row.get("change3M")),
                        jsonNumberToBigDecimal(row.get("change6M")),
                        jsonNumberToBigDecimal(row.get("change1Y"))
                ));
            }
            if (out.size() < symbolsRequested) {
                log.warn("MARKET_SUMMARY_PARTIAL symbolsRequested={} symbolsResolved={}", symbolsRequested, out.size());
            }
            return out;
        } catch (Exception ex) {
            log.warn("MARKET_SUMMARY_PARSE_FAILED reason={}", ex.toString());
            return Map.of();
        }
    }

    private static BigDecimal jsonNumberToBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private <T> Optional<T> readFromCache(String key, TypeReference<T> typeReference) {
        try {
            StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
            if (redis == null) {
                return Optional.empty();
            }
            String payload = redis.opsForValue().get(key);
            if (!StringUtils.hasText(payload)) {
                return Optional.empty();
            }
            T value = objectMapper.readValue(payload, typeReference);
            return Optional.of(value);
        } catch (Exception ex) {
            log.debug("MARKET_OVERVIEW_CACHE_READ_FAIL key={} reason={}", key, ex.toString());
            return Optional.empty();
        }
    }

    private Optional<MarketOverviewPageResponse> readFromCache(String key) {
        return readFromCache(key, new TypeReference<>() {
        });
    }

    private void writeToCache(String key, Object value) {
        try {
            StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
            if (redis == null) {
                return;
            }
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), CACHE_TTL);
        } catch (Exception ex) {
            log.debug("MARKET_OVERVIEW_CACHE_WRITE_FAIL key={} reason={}", key, ex.toString());
        }
    }

    private String cacheKey(int page, int size, String category, String search, String currency, String sort) {
        StringBuilder builder = new StringBuilder("market:overview:page:")
                .append(page)
                .append(":size:")
                .append(size)
                .append(":currency:")
                .append(currency);
        if (StringUtils.hasText(category)) {
            builder.append(":category:").append(category.toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(search)) {
            builder.append(":search:").append(search.toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(sort)) {
            builder.append(":sort:").append(sort.toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * Converts native listing quote into {@code X-Currency}. Spot metals and {@code *TRY} feeds are TRY-denominated.
     */
    private BigDecimal convertDisplayPrice(BigDecimal value, String symbol, String targetCurrency) {
        if (value == null) {
            return null;
        }
        String from = MarketOverviewCategoryRules.listingCurrency(symbol);
        return currencyConversionService.convert(value, from, targetCurrency);
    }

    private BigDecimal applyPrecision(BigDecimal value, String category) {
        if (value == null) {
            return null;
        }
        if ("CRYPTO".equalsIgnoreCase(category)) {
            return value.setScale(6, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MarketPriceDto(
            String symbol,
            BigDecimal price,
            String source,
            Instant timestamp,
            BigDecimal volume24h,
            BigDecimal openInterest,
            BigDecimal dayOpen,
            BigDecimal dayHigh,
            BigDecimal dayLow,
            String exchangeName,
            String underlyingSymbol,
            Instant contractExpiry,
            String linkedSpotSymbol,
            BigDecimal spotSpreadPct,
            BigDecimal spotSpreadAbs
            ) {

    }

    private record AnalyticsApiResponse<T>(
            boolean success,
            T data
            ) {

    }

    private record AnalyticsCandleDto(
            LocalDate candleDate,
            Instant openTime,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close
            ) {

        Instant timeAnchor() {
            if (openTime != null) {
                return openTime;
            }
            if (candleDate != null) {
                return candleDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
            }
            return Instant.EPOCH;
        }
    }

    private record AnalyticsMetrics(
            BigDecimal change24h,
            BigDecimal high24h,
            BigDecimal low24h
            ) {

        static AnalyticsMetrics empty() {
            return new AnalyticsMetrics(null, null, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record FxRateWire(
            String symbol,
            BigDecimal bid,
            BigDecimal ask,
            BigDecimal mid,
            String source,
            Instant timestamp
    ) {}

    private record MarketBaseItem(
            String symbol,
            String name,
            BigDecimal price,
            String category,
            Long instrumentId,
            String source,
            BigDecimal volume24h,
            BigDecimal openInterest,
            BigDecimal dayOpen,
            BigDecimal dayHigh,
            BigDecimal dayLow,
            String exchangeName,
            String underlyingSymbol,
            Instant contractExpiry,
            String linkedSpotSymbol,
            BigDecimal spotSpreadPct,
            BigDecimal spotSpreadAbs
    ) {}

    private record SummaryDto(
            BigDecimal price,
            BigDecimal change1D,
            BigDecimal change1M,
            BigDecimal change3M,
            BigDecimal change6M,
            BigDecimal change1Y
            ) {

    }

    private record HistoricalChanges(
            BigDecimal change1D,
            BigDecimal change1M,
            BigDecimal change3M,
            BigDecimal change6M,
            BigDecimal change1Y
            ) {

        static HistoricalChanges empty() {
            return new HistoricalChanges(null, null, null, null, null);
        }
    }
}
