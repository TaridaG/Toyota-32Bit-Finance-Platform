package com.company.newsservice.service.impl;

import com.company.newsservice.domain.NewsArticle;
import com.company.newsservice.domain.enums.NewsCategory;
import com.company.newsservice.dto.NewsDetailResponse;
import com.company.newsservice.dto.NewsResponse;
import com.company.newsservice.exception.ResourceNotFoundException;
import com.company.newsservice.repository.NewsArticleRepository;
import com.company.newsservice.service.NewsQueryService;
import com.company.newsservice.service.translation.NewsTranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NewsQueryServiceImpl implements NewsQueryService {

    private final NewsArticleRepository newsArticleRepository;
    private final NewsTranslationService newsTranslationService;

    @Override
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
        Map<Long, NewsTranslationService.NewsTextProjection> translations =
                newsTranslationService.resolveBatch(page.getContent(), language);
        return page.map(article -> toResponse(article, translations.get(article.getId()), includeOriginal));
    }

    @Override
    public NewsDetailResponse getById(Long id) {
        NewsArticle article = newsArticleRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("News article not found: " + id));

        return new NewsDetailResponse(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
                article.getArticleUrl(),
                article.getSourceName(),
                article.getCategory(),
                article.getPublishedAt(),
                article.getCreatedAt(),
                article.getUpdatedAt()
        );
    }

    private NewsResponse toResponse(
            NewsArticle article,
            NewsTranslationService.NewsTextProjection projection,
            boolean includeOriginal
    ) {
        NewsTranslationService.NewsTextProjection p = projection == null
                ? new NewsTranslationService.NewsTextProjection(
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
                article.getArticleUrl(),
                article.getSourceName(),
                article.getCategory(),
                article.getPublishedAt()
        );
    }

    private String normalize(String q) {
        return (q == null || q.isBlank()) ? null : q.trim();
    }
}