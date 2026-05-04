package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.dto.MarketInsightsResponse;
import com.company.finance_api.dto.MarketOverviewItemResponse;
import com.company.finance_api.dto.MarketOverviewPageResponse;
import com.company.finance_api.repository.InstrumentPriceRepository;
import com.company.finance_api.service.CurrencyConversionService;
import com.company.finance_api.service.InstrumentService;
import com.company.finance_api.service.MarketOverviewService;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
    private static final String INSIGHTS_CACHE_KEY = "market:insights";
    private static final String USD = "USD";
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
    public MarketOverviewPageResponse getOverview(int page, int size, String category, String search, String targetCurrency) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.max(size, 1);
        String normalizedCategory = normalize(category);
        String normalizedSearch = normalize(search);
        String normalizedCurrency = currencyConversionService.normalizeCurrency(targetCurrency);

        String cacheKey = cacheKey(resolvedPage, resolvedSize, normalizedCategory, normalizedSearch, normalizedCurrency);
        Optional<MarketOverviewPageResponse> cached = readFromCache(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        List<MarketBaseItem> merged = loadMergedBaseItems().stream()
                .filter(item -> categoryMatches(item, normalizedCategory))
                .filter(item -> searchMatches(item, normalizedSearch))
                .sorted(Comparator.comparing(MarketBaseItem::symbol, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int totalElements = merged.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / resolvedSize);
        int start = resolvedPage * resolvedSize;
        int end = Math.min(start + resolvedSize, totalElements);
        if (start >= totalElements) {
            MarketOverviewPageResponse emptyPage = new MarketOverviewPageResponse(List.of(), resolvedPage, resolvedSize, totalElements, totalPages);
            writeToCache(cacheKey, emptyPage);
            return emptyPage;
        }

        List<MarketBaseItem> currentPage = merged.subList(start, end);
        Map<String, BigDecimal> currentPricesBySymbol = currentPage.stream()
                .collect(Collectors.toMap(
                        MarketBaseItem::symbol,
                        MarketBaseItem::price,
                        (left, right) -> left
                ));
        Map<String, HistoricalChanges> changesBySymbol = fetchHistoricalChanges(currentPricesBySymbol);

        List<CompletableFuture<MarketOverviewItemResponse>> futures = currentPage.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> enrichWithAnalytics(
                        item,
                        normalizedCurrency,
                        changesBySymbol.getOrDefault(item.symbol(), HistoricalChanges.empty())
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
        Optional<MarketInsightsResponse> cached = readFromCache(currencyInsightsCacheKey, new TypeReference<>() {});
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
        Map<String, HistoricalChanges> changesBySymbol = fetchHistoricalChanges(currentPricesBySymbol);
        List<MarketOverviewItemResponse> all = enrichAll(baseItems, normalizedCurrency, changesBySymbol);
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
            Map<String, HistoricalChanges> changesBySymbol
    ) {
        List<CompletableFuture<MarketOverviewItemResponse>> futures = baseItems.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> enrichWithAnalytics(
                        item,
                        targetCurrency,
                        changesBySymbol.getOrDefault(item.symbol(), HistoricalChanges.empty())
                )))
                .toList();
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private List<MarketBaseItem> loadMergedBaseItems() {
        List<MarketPriceDto> prices = fetchLatestPrices();
        Map<String, Instrument> instrumentsBySymbol = instrumentService.getAllActive()
                .stream()
                .collect(Collectors.toMap(
                        instrument -> instrument.getSymbol().trim().toUpperCase(Locale.ROOT),
                        Function.identity(),
                        (left, right) -> left
                ));
        log.warn("items before filter: {}", prices.size());
        List<MarketBaseItem> filtered = prices.stream()
                .filter(price -> price.symbol() != null && !price.symbol().isBlank())
                .map(price -> {
                    String symbol = price.symbol().trim().toUpperCase(Locale.ROOT);
                    Instrument instrument = instrumentsBySymbol.get(symbol);
                    return mergeBase(
                            new MarketPriceDto(symbol, price.price(), price.source(), price.timestamp()),
                            instrument
                    );
                })
                .toList();
        log.warn("items after filter: {}", filtered.size());
        if (!filtered.isEmpty()) {
            return filtered;
        }
        log.warn("Market overview empty, returning fallback minimal dataset");
        return fallbackMinimalItems(prices, instrumentsBySymbol);
    }

    private MarketBaseItem mergeBase(MarketPriceDto price, Instrument instrument) {
        if (instrument == null) {
            return new MarketBaseItem(
                    price.symbol(),
                    price.symbol(),
                    price.price(),
                    null,
                    null
            );
        }
        return new MarketBaseItem(
                price.symbol(),
                instrument.getName(),
                price.price(),
                instrument.getType().name(),
                instrument.getId()
        );
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
                    if (instrument == null) {
                        return new MarketBaseItem(symbol, symbol, BigDecimal.ZERO, null, null);
                    }
                    return new MarketBaseItem(
                            symbol,
                            instrument.getName(),
                            BigDecimal.ZERO,
                            instrument.getType().name(),
                            instrument.getId()
                    );
                })
                .distinct()
                .toList();
        if (!fromPriceSymbols.isEmpty()) {
            return fromPriceSymbols;
        }
        return instrumentService.getAllActive().stream()
                .map(instrument -> new MarketBaseItem(
                        instrument.getSymbol(),
                        instrument.getName(),
                        BigDecimal.ZERO,
                        instrument.getType().name(),
                        instrument.getId()
                ))
                .toList();
    }

    private boolean categoryMatches(MarketBaseItem item, String category) {
        if (!StringUtils.hasText(category)) {
            return true;
        }
        return category.equalsIgnoreCase(item.category());
    }

    private boolean searchMatches(MarketBaseItem item, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }
        String lowered = search.toLowerCase(Locale.ROOT);
        return item.symbol().toLowerCase(Locale.ROOT).contains(lowered)
                || item.name().toLowerCase(Locale.ROOT).contains(lowered);
    }

    private MarketOverviewItemResponse enrichWithAnalytics(
            MarketBaseItem base,
            String targetCurrency,
            HistoricalChanges historicalChanges
    ) {
        try {
            List<AnalyticsCandleDto> candles = fetchCandles(base.symbol());
            AnalyticsMetrics metrics = computeMetrics(base.price(), candles);
            BigDecimal convertedPrice = applyPrecision(convertFromUsd(base.price(), targetCurrency), base.category());
            BigDecimal convertedHigh = applyPrecision(convertFromUsd(metrics.high24h(), targetCurrency), base.category());
            BigDecimal convertedLow = applyPrecision(convertFromUsd(metrics.low24h(), targetCurrency), base.category());
            BigDecimal change24h = metrics.change24h() == null ? historicalChanges.change1D() : metrics.change24h();
            return new MarketOverviewItemResponse(
                    base.symbol(),
                    base.name(),
                    convertedPrice,
                    change24h,
                    historicalChanges.change1D(),
                    historicalChanges.change1M(),
                    historicalChanges.change3M(),
                    historicalChanges.change6M(),
                    historicalChanges.change1Y(),
                    convertedHigh,
                    convertedLow,
                    base.category(),
                    base.instrumentId()
            );
        } catch (Exception ex) {
            log.warn("MARKET_OVERVIEW_ANALYTICS_FALLBACK symbol={} reason={}", base.symbol(), ex.toString());
            BigDecimal convertedPrice = applyPrecision(convertFromUsd(base.price(), targetCurrency), base.category());
            return new MarketOverviewItemResponse(
                    base.symbol(),
                    base.name(),
                    convertedPrice,
                    historicalChanges.change1D(),
                    historicalChanges.change1D(),
                    historicalChanges.change1M(),
                    historicalChanges.change3M(),
                    historicalChanges.change6M(),
                    historicalChanges.change1Y(),
                    null,
                    null,
                    base.category(),
                    base.instrumentId()
            );
        }
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

    private List<MarketPriceDto> fetchLatestPrices() {
        String url = UriComponentsBuilder.fromHttpUrl(marketDataBaseUrl)
                .path("/api/market/prices")
                .toUriString();
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
                            item.instrumentId()
                    );
                })
                .toList();
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
        String url = UriComponentsBuilder.fromHttpUrl(marketDataBaseUrl)
                .path("/api/market/prices/summary")
                .queryParam("symbols", String.join(",", symbols))
                .toUriString();
        try {
            Map<String, SummaryDto> body = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return body == null ? Map.of() : body;
        } catch (Exception ex) {
            log.warn("MARKET_SUMMARY_FALLBACK_FAILED reason={}", ex.toString());
            return Map.of();
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

    private String cacheKey(int page, int size, String category, String search, String currency) {
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
        return builder.toString();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal convertFromUsd(BigDecimal value, String targetCurrency) {
        if (value == null) {
            return null;
        }
        return currencyConversionService.convert(value, USD, targetCurrency);
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

    private record MarketPriceDto(
            String symbol,
            BigDecimal price,
            String source,
            Instant timestamp
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

    private record MarketBaseItem(
            String symbol,
            String name,
            BigDecimal price,
            String category,
            Long instrumentId
    ) {
    }

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
