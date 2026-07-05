package com.company.newsservice.translation.application;

import com.company.newsservice.bootstrap.config.NewsProperties;
import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.translation.domain.NewsArticleTranslation;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import com.company.newsservice.translation.infrastructure.persistence.NewsArticleTranslationRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import com.company.newsservice.translation.infrastructure.provider.NewsTranslationProvider;
import com.company.newsservice.translation.infrastructure.provider.NoopNewsTranslationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Haber başlık ve özet çevirilerini cache'ler, provider üzerinden üretir ve backfill job'ına destek verir.
 */
@Component
public class TranslateNewsUseCase {

    private static final Logger log = LoggerFactory.getLogger(TranslateNewsUseCase.class);

    private final NewsProperties newsProperties;
    private final NewsArticleRepository newsArticleRepository;
    private final NewsArticleTranslationRepository translationRepository;
    private final Map<String, NewsTranslationProvider> providerById;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter providerSuccessCounter;
    private final Counter providerFallbackCounter;

    public TranslateNewsUseCase(
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

    /** Yapılandırılmış desteklenen dil kodlarını normalize ederek döner. */
    public List<String> supportedLanguages() {
        List<String> configured = newsProperties.getTranslation().getSupportedLanguages();
        if (configured == null || configured.isEmpty()) {
            return List.of(resolveRequestedLanguage(newsProperties.getTranslation().getDefaultLanguage()));
        }
        return configured.stream().map(this::resolveRequestedLanguage).distinct().toList();
    }

    /** İstenen dili desteklenen dillere göre normalize eder; geçersizse default döner. */
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

    /**
     * Makale koleksiyonu için dil bazlı çeviri projection'larını DB'den okur.
     * Çeviri üretimi yalnızca ingest ({@link #pretranslateForArticle}) ve backfill job'ında yapılır;
     * kullanıcı isteğinde provider çağrılmaz.
     */
    @Transactional(readOnly = true)
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

        for (NewsArticle article : articleList) {
            if (existingByArticleId.containsKey(article.getId())) {
                cacheHitCounter.increment();
            } else {
                cacheMissCounter.increment();
            }
        }

        return articleList.stream().collect(Collectors.toMap(
                NewsArticle::getId,
                article -> {
                    NewsArticleTranslation tr = existingByArticleId.get(article.getId());
                    if (tr == null) {
                        return asOriginalProjection(article);
                    }
                    if (isLikelyFailedTranslationCopy(article, tr.getTitleTranslated(), tr.getSummaryTranslated(), language)) {
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
            if (isLikelyFailedTranslationCopy(article, titleTranslated, summaryTranslated, language)) {
                log.warn("NEWS_TRANSLATION_SKIP_UNCHANGED_FOREIGN articleId={} lang={}", article.getId(), language);
                return null;
            }
            NewsArticleTranslation entity = new NewsArticleTranslation();
            entity.setNewsArticle(article);
            entity.setLanguageCode(language);
            entity.setTitleTranslated(titleTranslated);
            entity.setSummaryTranslated(summaryTranslated);
            return translationRepository.save(entity);
        } catch (DataIntegrityViolationException dup) {
            return translationRepository.findByNewsArticleIdAndLanguageCode(article.getId(), language).orElse(null);
        } catch (Exception ex) {
            log.warn("NEWS_TRANSLATION_SAVE_FAIL articleId={} lang={} reason={}", article.getId(), language, ex.toString());
            return saveFallback(article, language);
        }
    }

    private NewsArticleTranslation saveFallback(NewsArticle article, String language) {
        if (!"en".equals(language)) {
            log.warn("NEWS_TRANSLATION_FALLBACK_SKIP_NON_EN articleId={} lang={}", article.getId(), language);
            return null;
        }
        try {
            NewsArticleTranslation entity = new NewsArticleTranslation();
            entity.setNewsArticle(article);
            entity.setLanguageCode(language);
            entity.setTitleTranslated(trimToLength(article.getTitle(), 500));
            entity.setSummaryTranslated(trimToLength(nz(article.getSummary()), 2000));
            return translationRepository.save(entity);
        } catch (DataIntegrityViolationException dup) {
            return translationRepository.findByNewsArticleIdAndLanguageCode(article.getId(), language).orElse(null);
        } catch (Exception fallbackEx) {
            log.warn("NEWS_TRANSLATION_FALLBACK_FAIL articleId={} lang={} reason={}", article.getId(), language, fallbackEx.toString());
            return null;
        }
    }

    /** Yeni ingest edilen makale için tüm desteklenen dillerde ön çeviri üretir. */
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

    /**
     * Eksik çeviri satırlarını batch halinde doldurur.
     *
     * @return başarıyla oluşturulan çeviri satırı sayısı
     */
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

    /**
     * MyMemory (and similar) often return the source string unchanged on quota/errors; saveFallback used to persist that
     * as a non-English row, so the API reported translated=true while the user still saw English. We only treat it as a
     * failed translation when the heuristic source language differs from the requested target (e.g. en article, tr target).
     */
    private static boolean isLikelyFailedTranslationCopy(
            NewsArticle article,
            String titleTranslated,
            String summaryTranslated,
            String targetLang
    ) {
        if (targetLang == null || targetLang.isBlank() || "en".equals(targetLang)) {
            return false;
        }
        if (!isUnchangedFromOriginal(article, titleTranslated, summaryTranslated)) {
            return false;
        }
        String heuristicSource = heuristicSourceLanguage(nz(article.getTitle()) + " " + nz(article.getSummary()));
        return !heuristicSource.equals(targetLang);
    }

    private static boolean isUnchangedFromOriginal(NewsArticle article, String titleTranslated, String summaryTranslated) {
        return normalizeForCompare(trimToLength(nz(titleTranslated), 500))
                .equals(normalizeForCompare(trimToLength(article.getTitle(), 500)))
                && normalizeForCompare(trimToLength(nz(summaryTranslated), 2000))
                .equals(normalizeForCompare(trimToLength(nz(article.getSummary()), 2000)));
    }

    /**
     * MyMemory often swaps typographic apostrophes/quotes; without this, English source and "fake" de/tr rows
     * fail {@link String#equals} and poison rows survive (translated=true with English body).
     */
    private static String normalizeForCompare(String value) {
        String s = Normalizer.normalize(nz(value).trim(), Normalizer.Form.NFC);
        s = s.replace('\u2019', '\'')
                .replace('\u2018', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u00a0', ' ')
                .replace('\r', ' ')
                .replace("\u200b", "");
        s = s.replaceAll("\\s+", " ");
        return s.trim();
    }

    private static String heuristicSourceLanguage(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("ğ") || lower.contains("ü") || lower.contains("ş")
                || lower.contains("ı") || lower.contains("ö") || lower.contains("ç")) {
            return "tr";
        }
        if (lower.contains("ß") || lower.contains("ä")) {
            return "de";
        }
        return "en";
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

    /** API'ye dönen orijinal ve çevrilmiş başlık/özet metinleri. */
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
