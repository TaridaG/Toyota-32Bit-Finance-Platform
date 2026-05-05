package com.company.newsservice.service.translation;

import com.company.newsservice.config.NewsProperties;
import com.company.newsservice.domain.NewsArticle;
import com.company.newsservice.domain.NewsArticleTranslation;
import com.company.newsservice.repository.NewsArticleRepository;
import com.company.newsservice.repository.NewsArticleTranslationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NewsTranslationService {

    private static final Logger log = LoggerFactory.getLogger(NewsTranslationService.class);

    private final NewsProperties newsProperties;
    private final NewsArticleRepository newsArticleRepository;
    private final NewsArticleTranslationRepository translationRepository;
    private final Map<String, NewsTranslationProvider> providerById;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter providerSuccessCounter;
    private final Counter providerFallbackCounter;

    public NewsTranslationService(
            NewsProperties newsProperties,
            NewsArticleRepository newsArticleRepository,
            NewsArticleTranslationRepository translationRepository,
            List<NewsTranslationProvider> providers,
            MeterRegistry meterRegistry
    ) {
        this.newsProperties = newsProperties;
        this.newsArticleRepository = newsArticleRepository;
        this.translationRepository = translationRepository;
        this.providerById = providers.stream().collect(Collectors.toMap(NewsTranslationProvider::providerId, Function.identity()));
        this.cacheHitCounter = meterRegistry.counter("news.translation.cache.hit");
        this.cacheMissCounter = meterRegistry.counter("news.translation.cache.miss");
        this.providerSuccessCounter = meterRegistry.counter("news.translation.provider.success");
        this.providerFallbackCounter = meterRegistry.counter("news.translation.provider.fallback");
    }

    public List<String> supportedLanguages() {
        List<String> configured = newsProperties.getTranslation().getSupportedLanguages();
        if (configured == null || configured.isEmpty()) {
            return List.of(resolveRequestedLanguage(newsProperties.getTranslation().getDefaultLanguage()));
        }
        return configured.stream().map(this::resolveRequestedLanguage).distinct().toList();
    }

    public String resolveRequestedLanguage(String requestedLanguage) {
        String candidate = normalizeLang(requestedLanguage);
        if (candidate.isBlank()) {
            candidate = normalizeLang(newsProperties.getTranslation().getDefaultLanguage());
        }
        if (candidate.isBlank()) {
            return "en";
        }
        List<String> supported = newsProperties.getTranslation().getSupportedLanguages();
        if (supported == null || supported.isEmpty()) {
            return candidate;
        }
        for (String language : supported) {
            if (candidate.equals(normalizeLang(language))) {
                return candidate;
            }
        }
        return normalizeLang(newsProperties.getTranslation().getDefaultLanguage());
    }

    @Transactional
    public Map<Long, NewsTextProjection> resolveBatch(Collection<NewsArticle> articles, String requestedLanguage) {
        if (articles == null || articles.isEmpty()) {
            return Map.of();
        }
        String language = resolveRequestedLanguage(requestedLanguage);
        List<NewsArticle> articleList = articles.stream().filter(Objects::nonNull).toList();

        boolean translationEnabled = newsProperties.getTranslation().isEnabled();
        if (!translationEnabled || language.isBlank()) {
            return articleList.stream().collect(Collectors.toMap(NewsArticle::getId, this::asOriginalProjection));
        }

        Map<Long, NewsArticleTranslation> existingByArticleId = translationRepository
                .findByNewsArticleIdInAndLanguageCode(articleList.stream().map(NewsArticle::getId).toList(), language)
                .stream()
                .collect(Collectors.toMap(item -> item.getNewsArticle().getId(), Function.identity()));

        NewsTranslationProvider provider = resolveProvider();
        for (NewsArticle article : articleList) {
            if (existingByArticleId.containsKey(article.getId())) {
                cacheHitCounter.increment();
                continue;
            }
            cacheMissCounter.increment();
            NewsArticleTranslation created = translateAndPersist(article, language, provider);
            if (created != null) {
                providerSuccessCounter.increment();
                existingByArticleId.put(article.getId(), created);
            } else {
                providerFallbackCounter.increment();
            }
        }

        return articleList.stream().collect(Collectors.toMap(
                NewsArticle::getId,
                article -> {
                    NewsArticleTranslation tr = existingByArticleId.get(article.getId());
                    if (tr == null) {
                        return asOriginalProjection(article);
                    }
                    return new NewsTextProjection(
                            article.getTitle(),
                            article.getSummary(),
                            tr.getTitleTranslated(),
                            tr.getSummaryTranslated(),
                            language,
                            true
                    );
                }
        ));
    }

    private NewsArticleTranslation translateAndPersist(NewsArticle article, String language, NewsTranslationProvider provider) {
        try {
            String titleTranslated = trimToLength(provider.translate(article.getTitle(), language), 500);
            String summaryTranslated = trimToLength(provider.translate(nz(article.getSummary()), language), 2000);
            if (titleTranslated.isBlank()) {
                return saveFallback(article, language);
            }
            NewsArticleTranslation entity = new NewsArticleTranslation();
            entity.setNewsArticle(article);
            entity.setLanguageCode(language);
            entity.setTitleTranslated(titleTranslated);
            entity.setSummaryTranslated(summaryTranslated);
            return translationRepository.save(entity);
        } catch (Exception ex) {
            log.warn("NEWS_TRANSLATION_SAVE_FAIL articleId={} lang={} reason={}", article.getId(), language, ex.toString());
            return saveFallback(article, language);
        }
    }

    private NewsArticleTranslation saveFallback(NewsArticle article, String language) {
        try {
            NewsArticleTranslation entity = new NewsArticleTranslation();
            entity.setNewsArticle(article);
            entity.setLanguageCode(language);
            entity.setTitleTranslated(trimToLength(article.getTitle(), 500));
            entity.setSummaryTranslated(trimToLength(nz(article.getSummary()), 2000));
            return translationRepository.save(entity);
        } catch (Exception fallbackEx) {
            log.warn("NEWS_TRANSLATION_FALLBACK_FAIL articleId={} lang={} reason={}", article.getId(), language, fallbackEx.toString());
            return null;
        }
    }

    @Transactional
    public void pretranslateForArticle(NewsArticle article) {
        if (article == null || article.getId() == null || !newsProperties.getTranslation().isEnabled()) {
            return;
        }
        NewsTranslationProvider provider = resolveProvider();
        for (String language : supportedLanguages()) {
            translationRepository.findByNewsArticleIdAndLanguageCode(article.getId(), language)
                    .ifPresentOrElse(
                            hit -> cacheHitCounter.increment(),
                            () -> {
                                cacheMissCounter.increment();
                                NewsArticleTranslation created = translateAndPersist(article, language, provider);
                                if (created != null) {
                                    providerSuccessCounter.increment();
                                } else {
                                    providerFallbackCounter.increment();
                                }
                            }
                    );
        }
    }

    @Transactional
    public int backfillMissingTranslations(int batchSize) {
        if (!newsProperties.getTranslation().isEnabled() || batchSize <= 0) {
            return 0;
        }
        int processed = 0;
        for (String language : supportedLanguages()) {
            List<NewsArticle> missing = newsArticleRepository
                    .findActiveWithoutTranslation(language, org.springframework.data.domain.PageRequest.of(0, batchSize))
                    .getContent();
            if (missing.isEmpty()) {
                continue;
            }
            NewsTranslationProvider provider = resolveProvider();
            for (NewsArticle article : missing) {
                cacheMissCounter.increment();
                NewsArticleTranslation created = translateAndPersist(article, language, provider);
                if (created != null) {
                    providerSuccessCounter.increment();
                    processed++;
                } else {
                    providerFallbackCounter.increment();
                }
            }
        }
        return processed;
    }

    private NewsTranslationProvider resolveProvider() {
        String configured = normalizeLang(newsProperties.getTranslation().getProvider());
        if (configured.isBlank()) {
            configured = "noop";
        }
        NewsTranslationProvider provider = providerById.get(configured);
        if (provider != null) {
            return provider;
        }
        return providerById.getOrDefault("noop", new NoopNewsTranslationProvider());
    }

    private NewsTextProjection asOriginalProjection(NewsArticle article) {
        return new NewsTextProjection(
                article.getTitle(),
                article.getSummary(),
                article.getTitle(),
                article.getSummary(),
                null,
                false
        );
    }

    private static String trimToLength(String value, int max) {
        String normalized = nz(value).trim();
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, max);
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeLang(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains(",")) {
            normalized = normalized.split(",")[0].trim();
        }
        if (normalized.contains("-")) {
            normalized = normalized.split("-")[0];
        }
        return normalized;
    }

    public record NewsTextProjection(
            String titleOriginal,
            String summaryOriginal,
            String titleTranslated,
            String summaryTranslated,
            String translatedLanguage,
            boolean translated
    ) {
    }
}
