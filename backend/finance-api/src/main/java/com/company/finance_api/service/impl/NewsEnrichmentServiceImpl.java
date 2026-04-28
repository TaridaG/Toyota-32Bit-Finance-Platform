package com.company.finance_api.service.impl;

import com.company.finance_api.dto.NewsEnrichedPageResponse;
import com.company.finance_api.dto.NewsEnrichedResponse;
import com.company.finance_api.service.InstrumentService;
import com.company.finance_api.service.NewsEnrichmentService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class NewsEnrichmentServiceImpl implements NewsEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(NewsEnrichmentServiceImpl.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);
    private static final Set<String> POSITIVE_KEYWORDS = Set.of(
            "gain", "gains", "rally", "rise", "rises", "up", "bull", "surge", "positive", "beat"
    );
    private static final Set<String> NEGATIVE_KEYWORDS = Set.of(
            "drop", "drops", "fall", "falls", "down", "bear", "sell", "negative", "miss", "loss"
    );

    private final InstrumentService instrumentService;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;
    private final RestClient restClient = RestClient.create();

    @Value("${clients.news.base-url:http://news-service:8080}")
    private String newsBaseUrl;

    @Value("${clients.analytics.base-url:http://analytics-service:8080}")
    private String analyticsBaseUrl;

    public NewsEnrichmentServiceImpl(
            InstrumentService instrumentService,
            ObjectMapper objectMapper,
            ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider
    ) {
        this.instrumentService = instrumentService;
        this.objectMapper = objectMapper;
        this.stringRedisTemplateProvider = stringRedisTemplateProvider;
    }

    @Override
    public NewsEnrichedPageResponse getEnrichedNews(int page, int size) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.max(size, 1);
        String cacheKey = "news:enriched:page:" + resolvedPage + ":size:" + resolvedSize;
        Optional<NewsEnrichedPageResponse> cached = readFromCache(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        NewsServicePageResponse<NewsServiceNewsItem> upstream = fetchNewsPage(resolvedPage, resolvedSize);
        List<String> symbols = instrumentService.getAllActive().stream()
                .map(instrument -> instrument.getSymbol().toUpperCase(Locale.ROOT))
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();

        List<NewsEnrichedResponse> enrichedContent = upstream.content().stream()
                .map(item -> enrich(item, symbols))
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

    private NewsEnrichedResponse enrich(NewsServiceNewsItem item, List<String> symbols) {
        String title = item.title() == null ? "" : item.title();
        String summary = item.summary() == null ? "" : item.summary();
        String bag = (title + " " + summary).toLowerCase(Locale.ROOT);
        List<String> relatedSymbols = extractSymbols(title + " " + summary, symbols);
        String sentiment = resolveSentiment(bag);
        BigDecimal reactionPercent1h = relatedSymbols.isEmpty() ? null : computeReactionPercent1h(relatedSymbols.get(0));

        return new NewsEnrichedResponse(
                item.id(),
                title,
                item.summary(),
                item.sourceName(),
                item.category(),
                item.publishedAt(),
                sentiment,
                relatedSymbols,
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

    private NewsServicePageResponse<NewsServiceNewsItem> fetchNewsPage(int page, int size) {
        String url = UriComponentsBuilder.fromHttpUrl(newsBaseUrl)
                .path("/api/news")
                .queryParam("page", page)
                .queryParam("size", size)
                .toUriString();

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

    private List<AnalyticsCandleDto> fetchCandles(String symbol) {
        String url = UriComponentsBuilder.fromHttpUrl(analyticsBaseUrl)
                .path("/api/analytics/instruments/{symbol}/candles")
                .queryParam("from", LocalDate.now().minusDays(1))
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

    private Optional<NewsEnrichedPageResponse> readFromCache(String key) {
        try {
            StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
            if (redis == null) {
                return Optional.empty();
            }
            String payload = redis.opsForValue().get(key);
            if (!StringUtils.hasText(payload)) {
                return Optional.empty();
            }
            NewsEnrichedPageResponse value = objectMapper.readValue(payload, new TypeReference<>() {
            });
            return Optional.of(value);
        } catch (Exception ex) {
            log.debug("NEWS_ENRICHED_CACHE_READ_FAIL key={} reason={}", key, ex.toString());
            return Optional.empty();
        }
    }

    private void writeToCache(String key, Object value) {
        try {
            StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
            if (redis == null) {
                return;
            }
            redis.opsForValue().set(key, objectMapper.writeValueAsString(value), CACHE_TTL);
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
            String sourceName,
            String category,
            Instant publishedAt
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
}
