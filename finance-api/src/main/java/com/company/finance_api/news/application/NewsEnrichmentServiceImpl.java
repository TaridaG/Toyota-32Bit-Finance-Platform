package com.company.finance_api.news.application;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.news.infrastructure.http.dto.NewsEnrichedDetailResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsEnrichedPageResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsEnrichedResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsOriginalResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsRelatedAssetPerformance;
import com.company.finance_api.news.infrastructure.http.dto.NewsWeeklyAssetRowResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsWeeklySourceRowResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsWeeklySummaryResponse;
import com.company.finance_api.news.infrastructure.http.dto.NewsWeeklyTopicRowResponse;
import com.company.finance_api.instrument.application.InstrumentService;
import com.company.finance_api.repository.InstrumentPriceRepository;
import com.company.finance_api.repository.NewsFavoriteRepository;
import com.company.finance_api.shared.cache.JsonCacheService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import com.company.finance_api.shared.web.ResourceNotFoundException;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/** News enrichment için response cache kullanan service implementation'dır. */
@Service
public class NewsEnrichmentServiceImpl implements NewsEnrichmentService {

  private static final Logger log = LoggerFactory.getLogger(NewsEnrichmentServiceImpl.class);
  private static final Duration CACHE_TTL = Duration.ofSeconds(10);
  private static final Duration ORIGINAL_CACHE_TTL = Duration.ofMinutes(10);
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
  private static final Set<String> POSITIVE_KEYWORDS =
      Set.of("gain", "gains", "rally", "rise", "rises", "up", "bull", "surge", "positive", "beat");
  private static final Set<String> NEGATIVE_KEYWORDS =
      Set.of("drop", "drops", "fall", "falls", "down", "bear", "sell", "negative", "miss", "loss");

  private static final int FAVORITE_NEWS_FETCH_CAP = 120;
  private static final int WEEKLY_SUMMARY_MAX_AGE_MINUTES = 7 * 24 * 60;
  private static final int WEEKLY_SUMMARY_TOP_LIMIT = 5;
  private static final List<String> WEEKLY_TOPIC_ORDER = List.of("crypto", "macro", "bist", "fx", "viop");

  private final InstrumentService instrumentService;
  private final InstrumentPriceRepository instrumentPriceRepository;
  private final NewsFavoriteRepository newsFavoriteRepository;
  private final CurrentUserResolver currentUserResolver;
  private final JsonCacheService jsonCacheService;
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
      JsonCacheService jsonCacheService) {
    this.instrumentService = instrumentService;
    this.instrumentPriceRepository = instrumentPriceRepository;
    this.newsFavoriteRepository = newsFavoriteRepository;
    this.currentUserResolver = currentUserResolver;
    this.jsonCacheService = jsonCacheService;
  }

  /** Filtrelenmiş ve enrichment uygulanmış news page sonucunu döner. */
  @Override
  public NewsEnrichedPageResponse getEnrichedNews(
      int page,
      int size,
      String language,
      String category,
      String sentiment,
      Integer maxAgeMinutes,
      String search,
      String relatedSymbols,
      String sourceName,
      String assetKey,
      String primaryTopic) {
    int resolvedPage = Math.max(page, 0);
    int resolvedSize = Math.max(size, 1);
    String resolvedLang = normalizeLanguage(language);
    String resolvedCategory = normalizeCategoryFilter(category);
    String resolvedSentiment = normalizeSentimentFilter(sentiment);
    Integer resolvedMaxAgeMinutes = normalizeMaxAge(maxAgeMinutes);
    String resolvedSearch = normalizeSearch(search);
    Set<String> resolvedRelatedSymbols = normalizeRelatedSymbolsFilter(relatedSymbols);
    String resolvedSourceName = normalizeSourceNameFilter(sourceName);
    String resolvedAssetKey = normalizeAssetKeyFilter(assetKey);
    String resolvedPrimaryTopic = normalizePrimaryTopicFilter(primaryTopic);
    String cacheKey =
        "news:enriched:v3:lang:"
            + resolvedLang
            + ":page:"
            + resolvedPage
            + ":size:"
            + resolvedSize
            + ":category:"
            + resolvedCategory
            + ":sentiment:"
            + resolvedSentiment
            + ":maxAge:"
            + (resolvedMaxAgeMinutes == null ? "all" : resolvedMaxAgeMinutes)
            + ":q:"
            + (resolvedSearch == null ? "" : resolvedSearch.toLowerCase(Locale.ROOT))
            + ":relatedSymbols:"
            + (resolvedRelatedSymbols.isEmpty() ? "all" : String.join(",", resolvedRelatedSymbols))
            + ":sourceName:"
            + (resolvedSourceName == null ? "all" : resolvedSourceName)
            + ":assetKey:"
            + (resolvedAssetKey == null ? "all" : resolvedAssetKey)
            + ":primaryTopic:"
            + (resolvedPrimaryTopic == null ? "all" : resolvedPrimaryTopic);
    Optional<NewsEnrichedPageResponse> cached =
        jsonCacheService.get(cacheKey, new TypeReference<>() {});
    if (cached.isPresent()) {
      return cached.get();
    }
    boolean hasClientFilters =
        !"all".equals(resolvedCategory)
            || !"all".equals(resolvedSentiment)
            || resolvedMaxAgeMinutes != null
            || !resolvedRelatedSymbols.isEmpty()
            || resolvedSourceName != null
            || resolvedAssetKey != null
            || resolvedPrimaryTopic != null;
    NewsServicePageResponse<NewsServiceNewsItem> upstream =
        hasClientFilters
            ? collectFilteredUpstreamPage(
                resolvedPage,
                resolvedSize,
                resolvedLang,
                resolvedCategory,
                resolvedSentiment,
                resolvedMaxAgeMinutes,
                resolvedSearch,
                resolvedRelatedSymbols,
                resolvedSourceName,
                resolvedAssetKey,
                resolvedPrimaryTopic)
            : fetchNewsPage(resolvedPage, resolvedSize, resolvedLang, true, resolvedSearch);
    List<String> symbols;
    try {
      symbols =
          instrumentService.getAllActive().stream()
              .map(instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT))
              .sorted(Comparator.comparingInt(String::length).reversed())
              .toList();
    } catch (Exception ex) {
      log.warn("NEWS_INSTRUMENT_CATALOG_LOAD_FAIL reason={}", ex.toString());
      symbols = List.of();
    }

    final List<String> symbolsForEnrichment = symbols;
    List<NewsEnrichedResponse> enrichedContent =
        upstream.content().stream().map(item -> enrich(item, symbolsForEnrichment)).toList();

    NewsEnrichedPageResponse response =
        new NewsEnrichedPageResponse(
            enrichedContent,
            upstream.page(),
            upstream.size(),
            upstream.totalElements(),
            upstream.totalPages());
    jsonCacheService.put(cacheKey, response, CACHE_TTL);
    return response;
  }

  @Override
  public NewsWeeklySummaryResponse getWeeklySummary(String language, String portfolioSymbols) {
    String resolvedLang = normalizeLanguage(language);
    Set<String> resolvedPortfolioSymbols = normalizeRelatedSymbolsFilter(portfolioSymbols);
    String cacheKey =
        "news:weekly-summary:v1:lang:"
            + resolvedLang
            + ":portfolioSymbols:"
            + (resolvedPortfolioSymbols.isEmpty() ? "none" : String.join(",", resolvedPortfolioSymbols));
    Optional<NewsWeeklySummaryResponse> cached =
        jsonCacheService.get(cacheKey, new TypeReference<>() {});
    if (cached.isPresent()) {
      return cached.get();
    }

    List<NewsServiceNewsItem> weeklyItems =
        collectFilteredUpstreamItems(
            resolvedLang,
            "all",
            "all",
            WEEKLY_SUMMARY_MAX_AGE_MINUTES,
            null,
            Set.of(),
            null,
            null,
            null);

    long totalCount = weeklyItems.size();
    Map<String, Long> topicCounts = new LinkedHashMap<>();
    Map<String, Long> assetCounts = new LinkedHashMap<>();
    Map<String, Long> sourceCounts = new LinkedHashMap<>();
    long portfolioRelatedCount = 0L;

    for (NewsServiceNewsItem item : weeklyItems) {
      String primaryTopic = resolvePrimaryTopic(item);
      if (primaryTopic != null) {
        topicCounts.put(primaryTopic, topicCounts.getOrDefault(primaryTopic, 0L) + 1L);
      }

      for (String raw : item.relatedSymbols() == null ? List.<String>of() : item.relatedSymbols()) {
        String assetSymbol = normalizeSidebarAssetSymbol(raw);
        if (!assetSymbol.isBlank()) {
          assetCounts.put(assetSymbol, assetCounts.getOrDefault(assetSymbol, 0L) + 1L);
        }
      }

      String source = normalizeSourceDisplay(item.sourceName());
      if (source != null) {
        sourceCounts.put(source, sourceCounts.getOrDefault(source, 0L) + 1L);
      }

      if (!resolvedPortfolioSymbols.isEmpty()
          && matchesFilters(item, "all", "all", null, resolvedPortfolioSymbols, null, null, null)) {
        portfolioRelatedCount++;
      }
    }

    List<NewsWeeklyTopicRowResponse> topics =
        WEEKLY_TOPIC_ORDER.stream()
            .map(
                key ->
                    new NewsWeeklyTopicRowResponse(
                        key,
                        topicCounts.getOrDefault(key, 0L),
                        totalCount == 0
                            ? 0
                            : (int) Math.round((topicCounts.getOrDefault(key, 0L) * 100.0d) / totalCount)))
            .filter(row -> row.count() > 0)
            .toList();

    Comparator<Map.Entry<String, Long>> byCountDescThenKey =
        Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
            .reversed()
            .thenComparing(Map.Entry::getKey);

    List<NewsWeeklyAssetRowResponse> topAssets =
        assetCounts.entrySet().stream()
            .sorted(byCountDescThenKey)
            .limit(WEEKLY_SUMMARY_TOP_LIMIT)
            .map(entry -> new NewsWeeklyAssetRowResponse(entry.getKey(), entry.getValue()))
            .toList();

    List<NewsWeeklySourceRowResponse> sources =
        sourceCounts.entrySet().stream()
            .sorted(byCountDescThenKey)
            .limit(WEEKLY_SUMMARY_TOP_LIMIT)
            .map(entry -> new NewsWeeklySourceRowResponse(entry.getKey(), entry.getValue()))
            .toList();

    NewsWeeklySummaryResponse response =
        new NewsWeeklySummaryResponse(
            totalCount, topics, topAssets, sources, resolvedPortfolioSymbols.isEmpty() ? 0L : portfolioRelatedCount);
    jsonCacheService.put(cacheKey, response, CACHE_TTL);
    return response;
  }

  private NewsServicePageResponse<NewsServiceNewsItem> collectFilteredUpstreamPage(
      int page,
      int size,
      String language,
      String category,
      String sentiment,
      Integer maxAgeMinutes,
      String search,
      Set<String> relatedSymbols,
      String sourceName,
      String assetKey,
      String primaryTopic) {
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
        if (!matchesFilters(
            item, category, sentiment, maxAgeMinutes, relatedSymbols, sourceName, assetKey, primaryTopic)) {
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

  private List<NewsServiceNewsItem> collectFilteredUpstreamItems(
      String language,
      String category,
      String sentiment,
      Integer maxAgeMinutes,
      String search,
      Set<String> relatedSymbols,
      String sourceName,
      String assetKey,
      String primaryTopic) {
    final int scanPageSize = 200;
    List<NewsServiceNewsItem> items = new ArrayList<>();
    int upstreamPageIndex = 0;
    int upstreamTotalPages = Integer.MAX_VALUE;

    while (upstreamPageIndex < upstreamTotalPages) {
      NewsServicePageResponse<NewsServiceNewsItem> upstreamPage =
          fetchNewsPage(upstreamPageIndex, scanPageSize, language, true, search);
      upstreamTotalPages = Math.max(upstreamPage.totalPages(), upstreamPageIndex + 1);

      for (NewsServiceNewsItem item : upstreamPage.content()) {
        if (matchesFilters(
            item, category, sentiment, maxAgeMinutes, relatedSymbols, sourceName, assetKey, primaryTopic)) {
          items.add(item);
        }
      }

      upstreamPageIndex++;
      if (upstreamPage.content().isEmpty()) {
        break;
      }
    }

    return items;
  }

  /** Kullanıcının favorite news listesi için enrichment uygulanmış page sonucunu döner. */
  @Override
  public NewsEnrichedPageResponse getEnrichedFavoriteNews(
      int page, int size, String language, String category, Integer maxAgeMinutes, String search) {
    int resolvedPage = Math.max(page, 0);
    int resolvedSize = Math.max(size, 1);
    String resolvedLang = normalizeLanguage(language);
    String resolvedCategory = normalizeCategoryFilter(category);
    Integer resolvedMaxAgeMinutes = normalizeMaxAge(maxAgeMinutes);
    String resolvedSearch = normalizeSearch(search);
    UUID userId = currentUserResolver.getCurrentUserId();
    List<Long> favoriteIds =
        newsFavoriteRepository.findByUserIdAndActiveTrue(userId).stream()
            .map(com.company.finance_api.domain.NewsFavorite::getNewsId)
            .limit(FAVORITE_NEWS_FETCH_CAP)
            .toList();

    List<NewsServiceNewsItem> filteredItems = new ArrayList<>();
    for (Long newsId : favoriteIds) {
      NewsServiceNewsItem item = fetchFavoriteNewsItem(newsId, resolvedLang);
      if (item == null) {
        continue;
      }
      if (!matchesFilters(
          item, resolvedCategory, "all", resolvedMaxAgeMinutes, Set.of(), null, null, null)) {
        continue;
      }
      if (!matchesSearch(item, resolvedSearch)) {
        continue;
      }
      filteredItems.add(item);
    }

    long totalElements = filteredItems.size();
    int totalPages =
        totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / resolvedSize);
    int fromIndex = Math.min(resolvedPage * resolvedSize, filteredItems.size());
    int toIndex = Math.min(fromIndex + resolvedSize, filteredItems.size());
    List<NewsServiceNewsItem> pageItems = filteredItems.subList(fromIndex, toIndex);

    List<String> symbols;
    try {
      symbols =
          instrumentService.getAllActive().stream()
              .map(instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT))
              .sorted(Comparator.comparingInt(String::length).reversed())
              .toList();
    } catch (Exception ex) {
      log.warn("NEWS_INSTRUMENT_CATALOG_LOAD_FAIL reason={}", ex.toString());
      symbols = List.of();
    }
    final List<String> symbolsForEnrichment = symbols;
    List<NewsEnrichedResponse> enrichedContent =
        pageItems.stream().map(item -> enrich(item, symbolsForEnrichment)).toList();

    return new NewsEnrichedPageResponse(
        enrichedContent, resolvedPage, resolvedSize, totalElements, totalPages);
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
        detail.topicTags());
  }

  private boolean matchesSearch(NewsServiceNewsItem item, String search) {
    if (!StringUtils.hasText(search)) {
      return true;
    }
    String bag = (nz(item.title()) + " " + nz(item.summary())).toLowerCase(Locale.ROOT);
    return bag.contains(search.toLowerCase(Locale.ROOT));
  }

  /** Chart ekranındaki zaman aralığına karşılık gelen enriched news listesini döner. */
  @Override
  public List<NewsEnrichedResponse> getEnrichedChartNews(
      String symbol,
      String categoryUi,
      Instant fromInclusive,
      Instant toInclusive,
      String language) {
    if (!StringUtils.hasText(symbol)
        || fromInclusive == null
        || toInclusive == null
        || toInclusive.isBefore(fromInclusive)) {
      return List.of();
    }
    String resolvedLang = normalizeLanguage(language);
    String resolvedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
    String resolvedCategoryUi = normalizeCategoryFilter(categoryUi);
    String cacheKey =
        "news:chart:lang:"
            + resolvedLang
            + ":symbol:"
            + resolvedSymbol
            + ":category:"
            + resolvedCategoryUi
            + ":from:"
            + fromInclusive
            + ":to:"
            + toInclusive;
    Optional<List<NewsEnrichedResponse>> cached =
        jsonCacheService.get(cacheKey, new TypeReference<>() {});
    if (cached.isPresent()) {
      return cached.get();
    }
    List<NewsServiceNewsItem> upstream = fetchNewsChart(fromInclusive, toInclusive, resolvedLang);
    List<String> catalogSymbols;
    try {
      catalogSymbols =
          instrumentService.getAllActive().stream()
              .map(instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT))
              .sorted(Comparator.comparingInt(String::length).reversed())
              .toList();
    } catch (Exception ex) {
      log.warn("NEWS_INSTRUMENT_CATALOG_LOAD_FAIL reason={}", ex.toString());
      catalogSymbols = List.of();
    }
    final List<String> symbolsForEnrichment = catalogSymbols;
    List<NewsEnrichedResponse> enriched =
        upstream.stream()
            .map(item -> enrich(item, symbolsForEnrichment))
            .filter(item -> matchesChartNews(item, resolvedSymbol, resolvedCategoryUi))
            .toList();
    jsonCacheService.put(cacheKey, enriched, Duration.ofMinutes(2));
    return enriched;
  }

  /** Tekil news kaydını related asset performance bilgileri ile birlikte döner. */
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
      catalogSymbols =
          instrumentService.getAllActive().stream()
              .map(instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT))
              .sorted(Comparator.comparingInt(String::length).reversed())
              .toList();
    } catch (Exception ex) {
      log.warn("NEWS_INSTRUMENT_CATALOG_LOAD_FAIL reason={}", ex.toString());
      catalogSymbols = List.of();
    }
    List<String> relatedSymbols =
        item.relatedSymbols() == null || item.relatedSymbols().isEmpty()
            ? extractSymbols(title + " " + summary, catalogSymbols)
            : List.copyOf(item.relatedSymbols());
    Map<String, String> namesBySymbol =
        instrumentService.getAllActive().stream()
            .collect(
                Collectors.toMap(
                    instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT),
                    Instrument::getName,
                    (left, right) -> left,
                    LinkedHashMap::new));
    List<NewsRelatedAssetPerformance> relatedAssets =
        relatedSymbols.stream()
            .map(
                symbol ->
                    buildRelatedAssetPerformance(
                        symbol, namesBySymbol.get(symbol.toUpperCase(Locale.ROOT))))
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
        relatedAssets);
  }

  /** Orijinal title/summary içeriğini kısa ömürlü cache ile birlikte döner. */
  @Override
  public NewsOriginalResponse getOriginalNews(Long id) {
    String cacheKey = "news:original:id:" + id;
    Optional<NewsOriginalResponse> cached = jsonCacheService.get(cacheKey, new TypeReference<>() {});
    if (cached.isPresent()) {
      return cached.get();
    }
    NewsServiceApiResponse<NewsServiceNewsDetailItem> body =
        restClient
            .get()
            .uri(
                UriComponentsBuilder.fromHttpUrl(newsBaseUrl)
                    .path("/api/news/{id}")
                    .queryParam("includeOriginal", true)
                    .buildAndExpand(id)
                    .toUriString())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    if (body == null || body.data() == null) {
      return new NewsOriginalResponse(id, "", "");
    }
    NewsServiceNewsDetailItem data = body.data();
    String originalTitle =
        StringUtils.hasText(data.titleOriginal()) ? data.titleOriginal() : data.title();
    String originalSummary =
        StringUtils.hasText(data.summaryOriginal()) ? data.summaryOriginal() : data.summary();
    NewsOriginalResponse response =
        new NewsOriginalResponse(data.id(), nz(originalTitle), nz(originalSummary));
    jsonCacheService.put(cacheKey, response, ORIGINAL_CACHE_TTL);
    return response;
  }

  private NewsEnrichedResponse enrich(NewsServiceNewsItem item, List<String> symbols) {
    String title = nz(item.title());
    String summary = nz(item.summary());
    String bag = (title + " " + summary).toLowerCase(Locale.ROOT);
    List<String> relatedSymbols =
        item.relatedSymbols() != null && !item.relatedSymbols().isEmpty()
            ? List.copyOf(item.relatedSymbols())
            : extractSymbols(title + " " + summary, symbols);
    String sentiment = resolveSentiment(bag);
    BigDecimal reactionPercent1h =
        relatedSymbols.isEmpty() ? null : computeReactionPercent1h(relatedSymbols.get(0));

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
        reactionPercent1h);
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

  private String resolvePrimaryTopic(NewsServiceNewsItem item) {
    for (String topic : resolveTopicTags(item)) {
      String normalized = normalizePrimaryTopicFilter(topic);
      if (normalized != null) {
        return normalized;
      }
    }
    return null;
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

  private static String normalizeSidebarAssetSymbol(String symbol) {
    if (!StringUtils.hasText(symbol)) {
      return "";
    }
    String upper = symbol.trim().toUpperCase(Locale.ROOT);
    if (upper.endsWith("USDT") && upper.length() > 4) {
      return upper.substring(0, upper.length() - 4);
    }
    return upper;
  }

  private static String normalizeSourceName(String sourceName) {
    if (!StringUtils.hasText(sourceName)) {
      return "";
    }
    return sourceName.trim().toLowerCase(Locale.ROOT);
  }

  private static String normalizeSourceDisplay(String sourceName) {
    if (!StringUtils.hasText(sourceName)) {
      return null;
    }
    String normalized = sourceName.trim();
    return normalized.isBlank() ? null : normalized;
  }

  private List<NewsServiceNewsItem> fetchNewsChart(
      Instant fromInclusive, Instant toInclusive, String language) {
    String url =
        UriComponentsBuilder.fromHttpUrl(newsBaseUrl)
            .path("/api/news/chart")
            .queryParam("from", fromInclusive)
            .queryParam("to", toInclusive)
            .queryParam("lang", language)
            .toUriString();
    try {
      NewsServiceApiResponse<List<NewsServiceNewsItem>> body =
          restClient.get().uri(url).retrieve().body(new ParameterizedTypeReference<>() {});
      if (body == null || body.data() == null) {
        return List.of();
      }
      return body.data();
    } catch (Exception ex) {
      log.warn(
          "NEWS_CHART_FETCH_FAIL from={} to={} reason={}",
          fromInclusive,
          toInclusive,
          ex.toString());
      return List.of();
    }
  }

  private boolean matchesFilters(
      NewsServiceNewsItem item,
      String category,
      String sentiment,
      Integer maxAgeMinutes,
      Set<String> relatedSymbols,
      String sourceName,
      String assetKey,
      String primaryTopic) {
    if (!"all".equals(category) && !matchesCategoryUi(item, category)) {
      return false;
    }
    if (!"all".equals(sentiment)) {
      String resolved =
          resolveSentiment((nz(item.title()) + " " + nz(item.summary())).toLowerCase(Locale.ROOT));
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
    if (!relatedSymbols.isEmpty()) {
      if (item.relatedSymbols() == null || item.relatedSymbols().isEmpty()) {
        return false;
      }
      boolean matched = false;
      for (String related : item.relatedSymbols()) {
        if (related != null && relatedSymbols.contains(normalizeSymbolKey(related))) {
          matched = true;
          break;
        }
      }
      if (!matched) {
        return false;
      }
    }
    if (sourceName != null && !sourceName.equals(normalizeSourceName(item.sourceName()))) {
      return false;
    }
    if (assetKey != null) {
      if (item.relatedSymbols() == null || item.relatedSymbols().isEmpty()) {
        return false;
      }
      boolean matched = false;
      for (String related : item.relatedSymbols()) {
        if (assetKey.equals(normalizeSidebarAssetSymbol(related))) {
          matched = true;
          break;
        }
      }
      if (!matched) {
        return false;
      }
    }
    if (primaryTopic != null) {
      String resolvedPrimaryTopic = resolvePrimaryTopic(item);
      if (!primaryTopic.equals(resolvedPrimaryTopic)) {
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
      return latest
          .subtract(previous)
          .multiply(BigDecimal.valueOf(100))
          .divide(previous, 4, RoundingMode.HALF_UP);
    } catch (Exception ex) {
      log.debug("NEWS_REACTION_CALC_FAIL symbol={} reason={}", symbol, ex.toString());
      return null;
    }
  }

  private NewsServicePageResponse<NewsServiceNewsItem> fetchNewsPage(
      int page, int size, String language, boolean includeOriginal, String search) {
    UriComponentsBuilder builder =
        UriComponentsBuilder.fromHttpUrl(newsBaseUrl)
            .path("/api/news")
            .queryParam("page", page)
            .queryParam("size", size)
            .queryParam("lang", language)
            .queryParam("includeOriginal", includeOriginal);
    if (StringUtils.hasText(search)) {
      builder.queryParam("q", search);
    }
    String url = builder.toUriString();

    NewsServiceApiResponse<NewsServicePageResponse<NewsServiceNewsItem>> body =
        restClient.get().uri(url).retrieve().body(new ParameterizedTypeReference<>() {});

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
        change1d =
            computePercentageChange(
                current, baselinePriceAt(symbol, now.minus(Duration.ofDays(1))));
      }
      if (change1w == null) {
        change1w =
            computePercentageChange(
                current, baselinePriceAt(symbol, now.minus(Duration.ofDays(7))));
      }
      if (change1m == null) {
        change1m =
            computePercentageChange(
                current, baselinePriceAt(symbol, now.minus(Duration.ofDays(30))));
      }
      if (change3m == null) {
        change3m =
            computePercentageChange(
                current, baselinePriceAt(symbol, now.minus(Duration.ofDays(90))));
      }
      if (change6m == null) {
        change6m =
            computePercentageChange(
                current, baselinePriceAt(symbol, now.minus(Duration.ofDays(180))));
      }
      if (change1y == null) {
        change1y =
            computePercentageChange(
                current, baselinePriceAt(symbol, now.minus(Duration.ofDays(365))));
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
          change1y);
    } catch (Exception ex) {
      log.debug("NEWS_RELATED_ASSET_PERF_FAIL symbol={} reason={}", symbol, ex.toString());
      return new NewsRelatedAssetPerformance(
          symbol, resolvedName, null, null, null, null, null, null, null);
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
    return current
        .subtract(baseline)
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
    String url =
        UriComponentsBuilder.fromHttpUrl(marketDataBaseUrl)
            .path("/api/market/prices/summary")
            .queryParam("symbols", String.join(",", symbols))
            .toUriString();
    try {
      Map<String, PriceSummaryDto> body =
          restClient.get().uri(url).retrieve().body(new ParameterizedTypeReference<>() {});
      return body == null ? Map.of() : body;
    } catch (Exception ex) {
      log.debug("NEWS_PRICE_SUMMARY_FAIL symbols={} reason={}", symbols, ex.toString());
      return Map.of();
    }
  }

  private NewsServiceNewsDetailItem fetchNewsDetail(Long id, String language) {
    String url =
        UriComponentsBuilder.fromHttpUrl(newsBaseUrl)
            .path("/api/news/{id}")
            .queryParam("lang", language)
            .queryParam("includeOriginal", true)
            .buildAndExpand(id)
            .toUriString();
    NewsServiceApiResponse<NewsServiceNewsDetailItem> body =
        restClient.get().uri(url).retrieve().body(new ParameterizedTypeReference<>() {});
    if (body == null || body.data() == null) {
      throw new ResourceNotFoundException("News article not found: " + id);
    }
    return body.data();
  }

  private List<AnalyticsCandleDto> fetchCandles(String symbol) {
    return fetchCandles(symbol, LocalDate.now().minusDays(2), LocalDate.now());
  }

  private List<AnalyticsCandleDto> fetchCandles(String symbol, LocalDate from, LocalDate to) {
    String url =
        UriComponentsBuilder.fromHttpUrl(analyticsBaseUrl)
            .path("/api/analytics/instruments/{symbol}/candles")
            .queryParam("from", from)
            .queryParam("to", to)
            .buildAndExpand(symbol)
            .toUriString();

    AnalyticsApiResponse<List<AnalyticsCandleDto>> body =
        restClient.get().uri(url).retrieve().body(new ParameterizedTypeReference<>() {});
    if (body == null || body.data() == null) {
      return List.of();
    }
    return body.data();
  }

  private record NewsServiceApiResponse<T>(boolean success, T data) {}

  private record NewsServicePageResponse<T>(
      List<T> content,
      @JsonAlias("number") int page,
      int size,
      long totalElements,
      int totalPages) {}

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
      List<String> topicTags) {}

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
      List<String> topicTags) {}

  private record AnalyticsApiResponse<T>(boolean success, T data) {}

  private record AnalyticsCandleDto(
      LocalDate candleDate, Instant openTime, BigDecimal high, BigDecimal low, BigDecimal close) {

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
    return Set.of("all", "bist", "viop", "fx", "crypto", "macro").contains(normalized)
        ? normalized
        : "all";
  }

  private static String normalizeSentimentFilter(String sentiment) {
    if (!StringUtils.hasText(sentiment)) {
      return "all";
    }
    String normalized = sentiment.trim().toLowerCase(Locale.ROOT);
    return Set.of("all", "positive", "negative", "neutral").contains(normalized)
        ? normalized
        : "all";
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

  private static String normalizeSourceNameFilter(String sourceName) {
    if (!StringUtils.hasText(sourceName)) {
      return null;
    }
    String normalized = normalizeSourceName(sourceName);
    return normalized.isBlank() ? null : normalized;
  }

  private static String normalizeAssetKeyFilter(String assetKey) {
    if (!StringUtils.hasText(assetKey)) {
      return null;
    }
    String normalized = normalizeSidebarAssetSymbol(assetKey);
    return normalized.isBlank() ? null : normalized;
  }

  private static String normalizePrimaryTopicFilter(String primaryTopic) {
    if (!StringUtils.hasText(primaryTopic)) {
      return null;
    }
    String normalized = primaryTopic.trim().toLowerCase(Locale.ROOT);
    return WEEKLY_TOPIC_ORDER.contains(normalized) ? normalized : null;
  }

  private static Set<String> normalizeRelatedSymbolsFilter(String relatedSymbols) {
    if (!StringUtils.hasText(relatedSymbols)) {
      return Set.of();
    }
    return java.util.Arrays.stream(relatedSymbols.split(","))
        .map(NewsEnrichmentServiceImpl::normalizeSymbolKey)
        .filter(StringUtils::hasText)
        .collect(Collectors.toCollection(LinkedHashSet::new));
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
      BigDecimal change1Y) {}
}
