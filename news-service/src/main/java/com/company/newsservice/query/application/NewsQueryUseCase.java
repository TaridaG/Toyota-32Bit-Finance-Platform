package com.company.newsservice.query.application;

import com.company.newsservice.query.domain.NewsArticle;
import com.company.newsservice.query.domain.enums.NewsCategory;
import com.company.newsservice.query.infrastructure.http.dto.NewsDetailResponse;
import com.company.newsservice.query.infrastructure.http.dto.NewsResponse;
import com.company.newsservice.shared.web.ResourceNotFoundException;
import com.company.newsservice.query.infrastructure.persistence.NewsArticleRepository;
import com.company.newsservice.shared.util.ArticleUrlNormalizer;
import com.company.newsservice.query.infrastructure.symbols.NewsRelatedSymbolsResolver;
import com.company.newsservice.ingestion.infrastructure.matching.NewsTopicTagger;
import com.company.newsservice.translation.application.TranslateNewsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Haber listeleme, chart overlay ve detay sorgularını çeviri projection'ları ile birlikte sunar.
 */
@Component
@RequiredArgsConstructor
public class NewsQueryUseCase {

    private static final int CHART_NEWS_MAX_ROWS = 2500;

    private final NewsArticleRepository newsArticleRepository;
    private final TranslateNewsUseCase newsTranslationService;
    private final NewsRelatedSymbolsResolver newsRelatedSymbolsResolver;
    private final NewsTopicTagger newsTopicTagger;

    /**
     * Kategori ve metin sorgusu ile sayfalanmış haber listesi döner; istenen dilde çeviri uygular.
     */
    public Page<NewsResponse> search(NewsCategory category, String q, Pageable pageable, String language, boolean includeOriginal) {
        String normalized = normalize(q);
        Page<NewsArticle> page;
        if (normalized == null) {
            page = newsArticleRepository.searchByCategory(category, pageable);
        } else if (category != null) {
            page = newsArticleRepository.searchByCategoryAndQuery(category, normalized, pageable);
        } else {
            page = newsArticleRepository.searchByQuery(normalized, pageable);
        }
        Map<Long, TranslateNewsUseCase.NewsTextProjection> translations =
                newsTranslationService.resolveBatch(page.getContent(), language);
        return page.map(article -> toResponse(article, translations.get(article.getId()), includeOriginal));
    }

    /**
     * Verilen zaman aralığındaki aktif haberleri chart overlay için listeler (üst sınır {@value #CHART_NEWS_MAX_ROWS}).
     */
    public List<NewsResponse> listForChart(Instant fromInclusive, Instant toInclusive, String language) {
        if (fromInclusive == null || toInclusive == null || toInclusive.isBefore(fromInclusive)) {
            return List.of();
        }
        List<NewsArticle> articles = newsArticleRepository.findActiveByPublishedAtBetween(fromInclusive, toInclusive);
        if (articles.size() > CHART_NEWS_MAX_ROWS) {
            articles = articles.subList(articles.size() - CHART_NEWS_MAX_ROWS, articles.size());
        }
        Map<Long, TranslateNewsUseCase.NewsTextProjection> translations =
                newsTranslationService.resolveBatch(articles, language);
        return articles.stream()
                .map(article -> toResponse(article, translations.get(article.getId()), false))
                .toList();
    }

    /**
     * Id ile aktif haber detayını döner; ilişkili sembolleri çözümler ve persist eder.
     */
    public NewsDetailResponse getById(Long id, String language, boolean includeOriginal) {
        NewsArticle article = newsArticleRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("News article not found: " + id));
        List<String> relatedSymbols = newsRelatedSymbolsResolver.resolveAndPersist(article);
        Map<Long, TranslateNewsUseCase.NewsTextProjection> translations =
                newsTranslationService.resolveBatch(List.of(article), language);
        TranslateNewsUseCase.NewsTextProjection projection = translations.get(article.getId());
        return toDetailResponse(article, projection, includeOriginal, relatedSymbols);
    }

    private NewsResponse toResponse(
            NewsArticle article,
            TranslateNewsUseCase.NewsTextProjection projection,
            boolean includeOriginal
    ) {
        List<String> relatedSymbols = newsRelatedSymbolsResolver.readOrMatch(article);
        TranslateNewsUseCase.NewsTextProjection p = projection == null
                ? new TranslateNewsUseCase.NewsTextProjection(
                article.getTitle(),
                article.getSummary(),
                article.getTitle(),
                article.getSummary(),
                null,
                false
        )
                : projection;
        return new NewsResponse(
                article.getId(),
                p.titleTranslated(),
                p.summaryTranslated(),
                includeOriginal ? p.titleOriginal() : null,
                includeOriginal ? p.summaryOriginal() : null,
                p.translatedLanguage(),
                p.translated(),
                normalizeArticleUrl(article),
                article.getImageUrl(),
                article.getSourceName(),
                article.getCategory(),
                article.getPublishedAt(),
                relatedSymbols,
                resolveTopicTags(article, relatedSymbols)
        );
    }

    private NewsDetailResponse toDetailResponse(
            NewsArticle article,
            TranslateNewsUseCase.NewsTextProjection projection,
            boolean includeOriginal,
            List<String> relatedSymbols
    ) {
        TranslateNewsUseCase.NewsTextProjection p = projection == null
                ? new TranslateNewsUseCase.NewsTextProjection(
                article.getTitle(),
                article.getSummary(),
                article.getTitle(),
                article.getSummary(),
                null,
                false
        )
                : projection;
        return new NewsDetailResponse(
                article.getId(),
                p.titleTranslated(),
                p.summaryTranslated(),
                includeOriginal ? p.titleOriginal() : null,
                includeOriginal ? p.summaryOriginal() : null,
                p.translatedLanguage(),
                p.translated(),
                normalizeArticleUrl(article),
                article.getImageUrl(),
                article.getSourceName(),
                article.getCategory(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt(),
                relatedSymbols,
                resolveTopicTags(article, relatedSymbols)
        );
    }

    private List<String> resolveTopicTags(NewsArticle article, List<String> relatedSymbols) {
        if (article.getTopicTags() != null && !article.getTopicTags().isEmpty()) {
            return List.copyOf(article.getTopicTags());
        }
        return newsTopicTagger.resolve(
                article.getCategory(),
                article.getTitle(),
                article.getSummary(),
                relatedSymbols
        );
    }

    private static String normalizeArticleUrl(NewsArticle article) {
        return ArticleUrlNormalizer.normalize(article.getArticleUrl(), null, article.getSourceName());
    }

    private String normalize(String q) {
        return (q == null || q.isBlank()) ? null : q.trim();
    }
}
