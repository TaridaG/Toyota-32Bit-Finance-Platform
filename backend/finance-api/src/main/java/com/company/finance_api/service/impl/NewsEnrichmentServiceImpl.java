package com.company.finance_api.service.impl;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.dto.NewsEnrichedDetailResponse;
import com.company.finance_api.dto.NewsEnrichedPageResponse;
import com.company.finance_api.dto.NewsEnrichedResponse;
import com.company.finance_api.dto.NewsOriginalResponse;
import com.company.finance_api.dto.NewsRelatedAssetPerformance;
import com.company.finance_api.exception.ResourceNotFoundException;
import com.company.finance_api.repository.InstrumentPriceRepository;
import com.company.finance_api.repository.NewsFavoriteRepository;
import com.company.finance_api.security.CurrentUserResolver;
import com.company.finance_api.service.InstrumentService;
import com.company.finance_api.service.NewsEnrichmentService;
import com.fasterxml.jackson.annotation.JsonAlias;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class NewsEnrichmentServiceImpl implements NewsEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(NewsEnrichmentServiceImpl.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);
    private static final Duration ORIGINAL_CACHE_TTL = Duration.ofMinutes(10);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final Set<String> POSITIVE_KEYWORDS = Set.of(
            "gain", "gains", "rally", "rise", "rises", "up", "bull", "surge", "positive", "beat"
    );
    private static final Set<String> NEGATIVE_KEYWORDS = Set.of(
            "drop", "drops", "fall", "falls", "down", "bear", "sell", "negative", "miss", "loss"
    );

    private static final int FAVORITE_NEWS_FETCH_CAP = 120;

    private final InstrumentService instrumentService;
    private final InstrumentPriceRepository instrumentPriceRepository;
    private final NewsFavoriteRepository newsFavoriteRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;
    private final RestClient restClient = RestClient.create();

    @Value("${clients.news.base-url:http://news-service:8080}")
    private String newsBaseUrl;

    @Value("${clients.analytics.base-url:http://analytics-service:8080}")
    private String analyticsBaseUrl;

    @Value("${clients.market-data.base-url:http://market-data-service:8080}")
    private String marketDataBaseUrl;

    public NewsEnrichmentServiceImpl(
            InstrumentService instrumentService,
            InstrumentPriceRepository instrumentPriceRepository,
            NewsFavoriteRepository newsFavoriteRepository,
            CurrentUserResolver currentUserResolver,
            ObjectMapper objectMapper,
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider
    ) {
        this.instrumentService = instrumentService;
        this.instrumentPriceRepository = instrumentPriceRepository;
        this.newsFavoriteRepository = newsFavoriteRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
        this.stringRedisTemplateProvider = stringRedisTemplateProvider;
    }

    @Override
    public NewsEnrichedPageResponse getEnrichedNews(
            int page,
            int size,
            String language,
            String category,
            String sentiment,
            Integer maxAgeMinutes,
            String search
    ) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.max(size, 1);
        String resolvedLang = normalizeLanguage(language);
        String resolvedCategory = normalizeCategoryFilter(category);
        String resolvedSentiment = normalizeSentimentFilter(sentiment);
        Integer resolvedMaxAgeMinutes = normalizeMaxAge(maxAgeMinutes);
        String resolvedSearch = normalizeSearch(search);
        String cacheKey = "news:enriched:v2:lang:" + resolvedLang
                + ":page:" + resolvedPage
                + ":size:" + resolvedSize
                + ":category:" + resolvedCategory
                + ":sentiment:" + resolvedSentiment
                + ":maxAge:" + (resolvedMaxAgeMinutes == null ? "all" : resolvedMaxAgeMinutes)
                + ":q:" + (resolvedSearch == null ? "" : resolvedSearch.toLowerCase(Locale.ROOT));
        Optional<NewsEnrichedPageResponse> cached = readFromCache(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }
        boolean hasClientFilters = !"all".equals(resolvedCategory)
                || !"all".equals(resolvedSentiment)
                || resolvedMaxAgeMinutes != null;
        NewsServicePageResponse<NewsServiceNewsItem> upstream = hasClientFilters
                ? collectFilteredUpstreamPage(
                        resolvedPage,
                        resolvedSize,
                        resolvedLang,
                        resolvedCategory,
                        resolvedSentiment,
                        resolvedMaxAgeMinutes,
                        resolvedSearch
                )
                : fetchNewsPage(resolvedPage, resolvedSize, resolvedLang, true, resolvedSearch);
        List<String> symbols;
        try {
            symbols = instrumentService.getAllActive().stream()
                    .map(instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT))
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .toList();
        } catch (Exception ex) {
            log.warn("NEWS_INSTRUMENT_CATALOG_LOAD_FAIL reason={}", ex.toString());
            symbols = List.of();
        }

        final List<String> symbolsForEnrichment = symbols;
        List<NewsEnrichedResponse> enrichedContent = upstream.content().stream()
                .map(item -> enrich(item, symbolsForEnrichment))
                .toList();

        NewsEnrichedPageResponse response = new NewsEnrichedPageResponse(
                enrichedContent,
                upstream.page(),
                upstream.size(),
                upstream.totalElements(),
                upstream.totalPages()
        );
        writeToCache(cacheKey, response);
        return response;
    }

    private NewsServicePageResponse<NewsServiceNewsItem> collectFilteredUpstreamPage(
            int page,
            int size,
            String language,
            String category,
            String sentiment,
            Integer maxAgeMinutes,
            String search
    ) {
        final int scanPageSize = Math.max(50, Math.min(200, size * 5));
        final long startIndex = (long) page * size;
        final long endExclusive = startIndex + size;

        List<NewsServiceNewsItem> selectedPageItems = new ArrayList<>();
        long filteredTotal = 0L;
        int upstreamPageIndex = 0;
        int upstreamTotalPages = Integer.MAX_VALUE;

        while (upstreamPageIndex < upstreamTotalPages) {
            NewsServicePageResponse<NewsServiceNewsItem> upstreamPage =
                    fetchNewsPage(upstreamPageIndex, scanPageSize, language, true, search);
            upstreamTotalPages = Math.max(upstreamPage.totalPages(), upstreamPageIndex + 1);

            for (NewsServiceNewsItem item : upstreamPage.content()) {
                if (!matchesFilters(item, category, sentiment, maxAgeMinutes)) {
                    continue;
                }
                if (filteredTotal >= startIndex && filteredTotal < endExclusive) {
                    selectedPageItems.add(item);
                }
                filteredTotal++;
            }

            upstreamPageIndex++;
            if (upstreamPage.content().isEmpty()) {
                break;
            }
        }

        int totalPages = filteredTotal == 0 ? 0 : (int) Math.ceil((double) filteredTotal / size);
        return new NewsServicePageResponse<>(selectedPageItems, page, size, filteredTotal, totalPages);
    }

    @Override
    public NewsEnrichedPageResponse getEnrichedFavoriteNews(
            int page,
            int size,
            String language,
            String category,
            Integer maxAgeMinutes,
            String search
    ) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.max(size, 1);
        String resolvedLang = normalizeLanguage(language);
        String resolvedCategory = normalizeCategoryFilter(category);
        Integer resolvedMaxAgeMinutes = normalizeMaxAge(maxAgeMinutes);
        String resolvedSearch = normalizeSearch(search);
        UUID userId = currentUserResolver.getCurrentUserId();
        List<Long> favoriteIds = newsFavoriteRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(com.company.finance_api.domain.NewsFavorite::getNewsId)
                .limit(FAVORITE_NEWS_FETCH_CAP)
                .toList();

        List<NewsServiceNewsItem> filteredItems = new ArrayList<>();
        for (Long newsId : favoriteIds) {
            NewsServiceNewsItem item = fetchFavoriteNewsItem(newsId, resolvedLang);
            if (item == null) {
                continue;
            }
            if (!matchesFilters(item, resolvedCategory, "all", resolvedMaxAgeMinutes)) {
                continue;
            }
            if (!matchesSearch(item, resolvedSearch)) {
                continue;
            }
            filteredItems.add(item);
        }

        long totalElements = filteredItems.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / resolvedSize);
        int fromIndex = Math.min(resolvedPage * resolvedSize, filteredItems.size());
        int toIndex = Math.min(fromIndex + resolvedSize, filteredItems.size());
        List<NewsServiceNewsItem> pageItems = filteredItems.subList(fromIndex, toIndex);

        List<String> symbols;
        try {
            symbols = instrumentService.getAllActive().stream()
                    .map(instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT))
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .toList();
        } catch (Exception ex) {
            log.warn("NEWS_INSTRUMENT_CATALOG_LOAD_FAIL reason={}", ex.toString());
            symbols = List.of();
        }
        final List<String> symbolsForEnrichment = symbols;
        List<NewsEnrichedResponse> enrichedContent = pageItems.stream()
                .map(item -> enrich(item, symbolsForEnrichment))
                .toList();

        return new NewsEnrichedPageResponse(
                enrichedContent,
                resolvedPage,
                resolvedSize,
                totalElements,
                totalPages
        );
    }

    private NewsServiceNewsItem fetchFavoriteNewsItem(Long newsId, String language) {
        try {
            NewsServiceNewsDetailItem detail = fetchNewsDetail(newsId, language);
            return toNewsItem(detail);
        } catch (ResourceNotFoundException ex) {
            return null;
        } catch (Exception ex) {
            log.debug("NEWS_FAVORITE_FETCH_FAIL id={} reason={}", newsId, ex.toString());
            return null;
        }
    }

    private static NewsServiceNewsItem toNewsItem(NewsServiceNewsDetailItem detail) {
        return new NewsServiceNewsItem(
                detail.id(),
                detail.title(),
                detail.summary(),
                detail.titleOriginal(),
                detail.summaryOriginal(),
                detail.translatedLanguage(),
                detail.translated(),
                detail.imageUrl(),
                detail.sourceName(),
                detail.category(),
                detail.publishedAt(),
                detail.relatedSymbols(),
                detail.topicTags()
        );
    }

    private boolean matchesSearch(NewsServiceNewsItem item, String search) {
        if (!StringUtils.hasText(search)) {
            return true;
        }
        String bag = (nz(item.title()) + " " + nz(item.summary())).toLowerCase(Locale.ROOT);
        return bag.contains(search.toLowerCase(Locale.ROOT));
    }

    @Override
    public List<NewsEnrichedResponse> getEnrichedChartNews(
            String symbol,
            String categoryUi,
            Instant fromInclusive,
            Instant toInclusive,
            String language
    ) {
        if (!StringUtils.hasText(symbol) || fromInclusive == null || toInclusive == null || toInclusive.isBefore(fromInclusive)) {
            return List.of();
        }
        String resolvedLang = normalizeLanguage(language);
        String resolvedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
        String resolvedCategoryUi = normalizeCategoryFilter(categoryUi);
        String cacheKey = "news:chart:lang:" + resolvedLang
                + ":symbol:" + resolvedSymbol
                + ":category:" + resolvedCategoryUi
                + ":from:" + fromInclusive
                + ":to:" + toInclusive;
        Optional<List<NewsEnrichedResponse>> cached = readFromCache(cacheKey, new TypeReference<>() {
        });
        if (cached.isPresent()) {
            return cached.get();
        }
        List<NewsServiceNewsItem> upstream = fetchNewsChart(fromInclusive, toInclusive, resolvedLang);
        List<String> catalogSymbols;
        try {
            catalogSymbols = instrumentService.getAllActive().stream()
                    .map(instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT))
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .toList();
        } catch (Exception ex) {
            log.warn("NEWS_INSTRUMENT_CATALOG_LOAD_FAIL reason={}", ex.toString());
            catalogSymbols = List.of();
        }
        final List<String> symbolsForEnrichment = catalogSymbols;
        List<NewsEnrichedResponse> enriched = upstream.stream()
                .map(item -> enrich(item, symbolsForEnrichment))
                .filter(item -> matchesChartNews(item, resolvedSymbol, resolvedCategoryUi))
                .toList();
        writeToCache(cacheKey, enriched, Duration.ofMinutes(2));
        return enriched;
    }

    @Override
    public NewsEnrichedDetailResponse getEnrichedNewsDetail(Long id, String language) {
        String resolvedLang = normalizeLanguage(language);
        NewsServiceNewsDetailItem item = fetchNewsDetail(id, resolvedLang);
        String title = nz(item.title());
        String summary = nz(item.summary());
        String bag = (title + " " + summary).toLowerCase(Locale.ROOT);
        String sentiment = resolveSentiment(bag);
        String categoryUi = mapCategoryToUi(item.category());
        List<String> catalogSymbols;
        try {
            catalogSymbols = instrumentService.getAllActive().stream()
                    .map(instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT))
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .toList();
        } catch (Exception ex) {
            log.warn("NEWS_INSTRUMENT_CATALOG_LOAD_FAIL reason={}", ex.toString());
            catalogSymbols = List.of();
        }
        List<String> relatedSymbols = item.relatedSymbols() == null || item.relatedSymbols().isEmpty()
                ? extractSymbols(title + " " + summary, catalogSymbols)
                : List.copyOf(item.relatedSymbols());
        Map<String, String> namesBySymbol = instrumentService.getAllActive().stream()
                .collect(Collectors.toMap(
                        instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT),
                        Instrument::getName,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        List<NewsRelatedAssetPerformance> relatedAssets = relatedSymbols.stream()
                .map(symbol -> buildRelatedAssetPerformance(symbol, namesBySymbol.get(symbol.toUpperCase(Locale.ROOT))))
                .toList();
        return new NewsEnrichedDetailResponse(
                item.id(),
                title,
                summary,
                item.titleOriginal(),
                item.summaryOriginal(),
                item.translatedLanguage(),
                item.translated(),
                item.articleUrl(),
                item.imageUrl(),
                item.sourceName(),
                item.category(),
                categoryUi,
                item.publishedAt(),
                sentiment,
                relatedSymbols,
                resolveTopicTags(item),
                relatedAssets
        );
    }

    @Override
    public NewsOriginalResponse getOriginalNews(Long id) {
        String cacheKey = "news:original:id:" + id;
        Optional<NewsOriginalResponse> cached = readFromCache(cacheKey, new TypeReference<>() {
        });
        if (cached.isPresent()) {
            return cached.get();
        }
        NewsServiceApiResponse<NewsServiceNewsDetailItem> body = restClient.get()
                .uri(UriComponentsBuilder.fromHttpUrl(newsBaseUrl)
                        .path("/api/news/{id}")
                        .queryParam("includeOriginal", true)
                        .buildAndExpand(id)
                        .toUriString())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (body == null || body.data() == null) {
            return new NewsOriginalResponse(id, "", "");
        }
        NewsServiceNewsDetailItem data = body.data();
        String originalTitle = StringUtils.hasText(data.titleOriginal()) ? data.titleOriginal() : data.title();
        String originalSummary = StringUtils.hasText(data.summaryOriginal()) ? data.summaryOriginal() : data.summary();
        NewsOriginalResponse response = new NewsOriginalResponse(
                data.id(),
                nz(originalTitle),
                nz(originalSummary)
        );
        writeToCache(cacheKey, response, ORIGINAL_CACHE_TTL);
        return response;
    }

    private NewsEnrichedResponse enrich(NewsServiceNewsItem item, List<String> symbols) {
        String title = nz(item.title());
        String summary = nz(item.summary());
        String bag = (title + " " + summary)
                .toLowerCase(Locale.ROOT);
        List<String> relatedSymbols = item.relatedSymbols() != null && !item.relatedSymbols().isEmpty()
                ? List.copyOf(item.relatedSymbols())
                : extractSymbols(title + " " + summary, symbols);
        String sentiment = resolveSentiment(bag);
        BigDecimal reactionPercent1h = relatedSymbols.isEmpty() ? null : computeReactionPercent1h(relatedSymbols.get(0));

        return new NewsEnrichedResponse(
                item.id(),
                title,
                summary,
                item.titleOriginal(),
                item.summaryOriginal(),
                item.translatedLanguage(),
                item.translated(),
                item.imageUrl(),
                item.sourceName(),
                item.category(),
                item.publishedAt(),
                sentiment,
                relatedSymbols,
                resolveTopicTags(item),
                reactionPercent1h
        );
    }

    private String resolveSentiment(String loweredText) {
        boolean hasPositive = POSITIVE_KEYWORDS.stream().anyMatch(loweredText::contains);
        boolean hasNegative = NEGATIVE_KEYWORDS.stream().anyMatch(loweredText::contains);
        if (hasPositive && !hasNegative) {
            return "positive";
        }
        if (hasNegative && !hasPositive) {
            return "negative";
        }
        return "neutral";
    }

    /** Match by symbol, any topic tag, or legacy primary category. */
    private boolean matchesChartNews(NewsEnrichedResponse item, String symbol, String categoryUi) {
        if (hasRelatedSymbolMatch(item, symbol)) {
            return true;
        }
        return matchesCategoryUi(item, categoryUi);
    }

    private boolean matchesCategoryUi(NewsEnrichedResponse item, String categoryUi) {
        if ("all".equals(categoryUi)) {
            return true;
        }
        if (categoryUi.equals(mapCategoryToUi(item.category()))) {
            return true;
        }
        return item.topicTags() != null && item.topicTags().stream().anyMatch(categoryUi::equals);
    }

    private boolean matchesCategoryUi(NewsServiceNewsItem item, String categoryUi) {
        if ("all".equals(categoryUi)) {
            return true;
        }
        if (categoryUi.equals(mapCategoryToUi(item.category()))) {
            return true;
        }
        return item.topicTags() != null && item.topicTags().stream().anyMatch(categoryUi::equals);
    }

    private List<String> resolveTopicTags(NewsServiceNewsItem item) {
        return resolveTopicTags(item.topicTags(), item.category());
    }

    private List<String> resolveTopicTags(NewsServiceNewsDetailItem item) {
        return resolveTopicTags(item.topicTags(), item.category());
    }

    private List<String> resolveTopicTags(List<String> topicTags, String wireCategory) {
        if (topicTags != null && !topicTags.isEmpty()) {
            return List.copyOf(topicTags);
        }
        return List.of(mapCategoryToUi(wireCategory));
    }

    private boolean hasRelatedSymbolMatch(NewsEnrichedResponse item, String symbol) {
        if (item.relatedSymbols() == null || item.relatedSymbols().isEmpty()) {
            return false;
        }
        String target = normalizeSymbolKey(symbol);
        for (String related : item.relatedSymbols()) {
            if (related != null && normalizeSymbolKey(related).equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizeSymbolKey(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            return "";
        }
        return symbol.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private List<NewsServiceNewsItem> fetchNewsChart(Instant fromInclusive, Instant toInclusive, String language) {
        String url = UriComponentsBuilder.fromHttpUrl(newsBaseUrl)
                .path("/api/news/chart")
                .queryParam("from", fromInclusive)
                .queryParam("to", toInclusive)
                .queryParam("lang", language)
                .toUriString();
        try {
            NewsServiceApiResponse<List<NewsServiceNewsItem>> body = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (body == null || body.data() == null) {
                return List.of();
            }
            return body.data();
        } catch (Exception ex) {
            log.warn("NEWS_CHART_FETCH_FAIL from={} to={} reason={}", fromInclusive, toInclusive, ex.toString());
            return List.of();
        }
    }

    private boolean matchesFilters(NewsServiceNewsItem item, String category, String sentiment, Integer maxAgeMinutes) {
        if (!"all".equals(category) && !matchesCategoryUi(item, category)) {
            return false;
        }
        if (!"all".equals(sentiment)) {
            String resolved = resolveSentiment((nz(item.title()) + " " + nz(item.summary())).toLowerCase(Locale.ROOT));
            if (!sentiment.equals(resolved)) {
                return false;
            }
        }
        if (maxAgeMinutes != null) {
            if (item.publishedAt() == null) {
                return false;
            }
            Instant threshold = Instant.now().minusSeconds(maxAgeMinutes.longValue() * 60L);
            if (item.publishedAt().isBefore(threshold)) {
                return false;
            }
        }
        return true;
    }

    private static String mapCategoryToUi(String category) {
        String c = category == null ? "" : category.toUpperCase(Locale.ROOT);
        return switch (c) {
            case "CRYPTO" -> "crypto";
            case "FX" -> "fx";
            case "VIOP" -> "viop";
            case "STOCK" -> "bist";
            case "FUND", "BOND", "GENERAL_ECONOMY" -> "macro";
            default -> "macro";
        };
    }

    private List<String> extractSymbols(String text, List<String> knownSymbols) {
        String source = text == null ? "" : text.toUpperCase(Locale.ROOT);
        Set<String> related = new LinkedHashSet<>();
        for (String symbol : knownSymbols) {
            Pattern pattern = Pattern.compile("(^|[^A-Z0-9])" + Pattern.quote(symbol) + "([^A-Z0-9]|$)");
            if (pattern.matcher(source).find()) {
                related.add(symbol);
            }
            if (related.size() >= 6) {
                break;
            }
        }
        return new ArrayList<>(related);
    }

    private BigDecimal computeReactionPercent1h(String symbol) {
        try {
            List<AnalyticsCandleDto> candles = fetchCandles(symbol);
            if (candles.size() < 2) {
                return null;
            }
            List<AnalyticsCandleDto> sorted = new ArrayList<>(candles);
            sorted.sort(Comparator.comparing(AnalyticsCandleDto::timeAnchor));
            BigDecimal latest = sorted.get(sorted.size() - 1).close();
            BigDecimal previous = sorted.get(sorted.size() - 2).close();
            if (latest == null || previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
                return null;
            }
            return latest.subtract(previous)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previous, 4, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            log.debug("NEWS_REACTION_CALC_FAIL symbol={} reason={}", symbol, ex.toString());
            return null;
        }
    }

    private NewsServicePageResponse<NewsServiceNewsItem> fetchNewsPage(
            int page,
            int size,
            String language,
            boolean includeOriginal,
            String search
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(newsBaseUrl)
                .path("/api/news")
                .queryParam("page", page)
                .queryParam("size", size)
                .queryParam("lang", language)
                .queryParam("includeOriginal", includeOriginal);
        if (StringUtils.hasText(search)) {
            builder.queryParam("q", search);
        }
        String url = builder.toUriString();

        NewsServiceApiResponse<NewsServicePageResponse<NewsServiceNewsItem>> body = restClient.get()
                .uri(url)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (body == null || body.data() == null) {
            return new NewsServicePageResponse<>(List.of(), page, size, 0, 0);
        }
        return body.data();
    }

    private NewsRelatedAssetPerformance buildRelatedAssetPerformance(String symbol, String name) {
        String resolvedName = StringUtils.hasText(name) ? name : symbol;
        try {
            PriceSummaryDto summary = fetchPriceSummary(symbol);
            BigDecimal current = summary != null ? summary.price() : resolveLatestPrice(symbol);
            Instant now = Instant.now();
            BigDecimal change1d = summary != null ? summary.change1D() : null;
            BigDecimal change1w = summary != null ? summary.change1W() : null;
            BigDecimal change1m = summary != null ? summary.change1M() : null;
            BigDecimal change3m = summary != null ? summary.change3M() : null;
            BigDecimal change6m = summary != null ? summary.change6M() : null;
            BigDecimal change1y = summary != null ? summary.change1Y() : null;
            if (change1d == null) {
                change1d = computePercentageChange(current, baselinePriceAt(symbol, now.minus(Duration.ofDays(1))));
            }
            if (change1w == null) {
                change1w = computePercentageChange(current, baselinePriceAt(symbol, now.minus(Duration.ofDays(7))));
            }
            if (change1m == null) {
                change1m = computePercentageChange(current, baselinePriceAt(symbol, now.minus(Duration.ofDays(30))));
            }
            if (change3m == null) {
                change3m = computePercentageChange(current, baselinePriceAt(symbol, now.minus(Duration.ofDays(90))));
            }
            if (change6m == null) {
                change6m = computePercentageChange(current, baselinePriceAt(symbol, now.minus(Duration.ofDays(180))));
            }
            if (change1y == null) {
                change1y = computePercentageChange(current, baselinePriceAt(symbol, now.minus(Duration.ofDays(365))));
            }
            return new NewsRelatedAssetPerformance(
                    symbol,
                    resolvedName,
                    current,
                    change1d,
                    change1w,
                    change1m,
                    change3m,
                    change6m,
                    change1y
            );
        } catch (Exception ex) {
            log.debug("NEWS_RELATED_ASSET_PERF_FAIL symbol={} reason={}", symbol, ex.toString());
            return new NewsRelatedAssetPerformance(
                    symbol, resolvedName, null, null, null, null, null, null, null
            );
        }
    }

    private BigDecimal resolveLatestPrice(String symbol) {
        return baselinePriceAt(symbol, Instant.now());
    }

    private BigDecimal baselinePriceAt(String symbol, Instant target) {
        return instrumentPriceRepository
                .findLatestPricesAtOrBefore(List.of(symbol), PriceType.MARKET.name(), target)
                .stream()
                .findFirst()
                .map(InstrumentPriceRepository.SymbolPriceView::getPrice)
                .orElse(null);
    }

    private BigDecimal computePercentageChange(BigDecimal current, BigDecimal baseline) {
        if (current == null || baseline == null || baseline.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(baseline)
                .divide(baseline, 8, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private PriceSummaryDto fetchPriceSummary(String symbol) {
        Map<String, PriceSummaryDto> summaries = fetchPriceSummaries(List.of(symbol));
        return summaries.get(symbol);
    }

    private Map<String, PriceSummaryDto> fetchPriceSummaries(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return Map.of();
        }
        String url = UriComponentsBuilder.fromHttpUrl(marketDataBaseUrl)
                .path("/api/market/prices/summary")
                .queryParam("symbols", String.join(",", symbols))
                .toUriString();
        try {
            Map<String, PriceSummaryDto> body = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return body == null ? Map.of() : body;
        } catch (Exception ex) {
            log.debug("NEWS_PRICE_SUMMARY_FAIL symbols={} reason={}", symbols, ex.toString());
            return Map.of();
        }
    }

    private NewsServiceNewsDetailItem fetchNewsDetail(Long id, String language) {
        String url = UriComponentsBuilder.fromHttpUrl(newsBaseUrl)
                .path("/api/news/{id}")
                .queryParam("lang", language)
                .queryParam("includeOriginal", true)
                .buildAndExpand(id)
                .toUriString();
        NewsServiceApiResponse<NewsServiceNewsDetailItem> body = restClient.get()
                .uri(url)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (body == null || body.data() == null) {
            throw new ResourceNotFoundException("News article not found: " + id);
        }
        return body.data();
    }

    private List<AnalyticsCandleDto> fetchCandles(String symbol) {
        return fetchCandles(symbol, LocalDate.now().minusDays(2), LocalDate.now());
    }

    private List<AnalyticsCandleDto> fetchCandles(String symbol, LocalDate from, LocalDate to) {
        String url = UriComponentsBuilder.fromHttpUrl(analyticsBaseUrl)
                .path("/api/analytics/instruments/{symbol}/candles")
                .queryParam("from", from)
                .queryParam("to", to)
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

    private Optional<NewsEnrichedPageResponse> readFromCache(String key) {
        return readFromCache(key, new TypeReference<>() {
        });
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
            log.debug("NEWS_ENRICHED_CACHE_READ_FAIL key={} reason={}", key, ex.toString());
            return Optional.empty();
        }
    }

    private void writeToCache(String key, Object value) {
        writeToCache(key, value, CACHE_TTL);
    }

    private void writeToCache(String key, Object value, Duration ttl) {
        try {
            StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
            if (redis == null) {
                return;
            }
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception ex) {
            log.debug("NEWS_ENRICHED_CACHE_WRITE_FAIL key={} reason={}", key, ex.toString());
        }
    }

    private record NewsServiceApiResponse<T>(
            boolean success,
            T data
            ) {

    }

    private record NewsServicePageResponse<T>(
            List<T> content,
            @JsonAlias("number")
            int page,
            int size,
            long totalElements,
            int totalPages
            ) {

    }

    private record NewsServiceNewsItem(
            Long id,
            String title,
            String summary,
            String titleOriginal,
            String summaryOriginal,
            String translatedLanguage,
            boolean translated,
            String imageUrl,
            String sourceName,
            String category,
            Instant publishedAt,
            List<String> relatedSymbols,
            List<String> topicTags
            ) {

    }

    private record NewsServiceNewsDetailItem(
            Long id,
            String title,
            String summary,
            String titleOriginal,
            String summaryOriginal,
            String translatedLanguage,
            boolean translated,
            String articleUrl,
            String imageUrl,
            String sourceName,
            String category,
            Instant publishedAt,
            List<String> relatedSymbols,
            List<String> topicTags
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

    private static String normalizeLanguage(String language) {
        if (!StringUtils.hasText(language)) {
            return "en";
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains(",")) {
            normalized = normalized.split(",")[0].trim();
        }
        if (normalized.contains("-")) {
            normalized = normalized.split("-")[0];
        }
        return normalized.isBlank() ? "en" : normalized;
    }

    private static String normalizeCategoryFilter(String category) {
        if (!StringUtils.hasText(category)) {
            return "all";
        }
        String normalized = category.trim().toLowerCase(Locale.ROOT);
        return Set.of("all", "bist", "viop", "fx", "crypto", "macro").contains(normalized) ? normalized : "all";
    }

    private static String normalizeSentimentFilter(String sentiment) {
        if (!StringUtils.hasText(sentiment)) {
            return "all";
        }
        String normalized = sentiment.trim().toLowerCase(Locale.ROOT);
        return Set.of("all", "positive", "negative", "neutral").contains(normalized) ? normalized : "all";
    }

    private static Integer normalizeMaxAge(Integer maxAgeMinutes) {
        if (maxAgeMinutes == null || maxAgeMinutes <= 0) {
            return null;
        }
        return maxAgeMinutes;
    }

    private static String normalizeSearch(String search) {
        if (!StringUtils.hasText(search)) {
            return null;
        }
        String normalized = search.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private record PriceSummaryDto(
            BigDecimal price,
            BigDecimal change1D,
            BigDecimal change1W,
            BigDecimal change1M,
            BigDecimal change3M,
            BigDecimal change6M,
            BigDecimal change1Y
    ) {
    }
}
