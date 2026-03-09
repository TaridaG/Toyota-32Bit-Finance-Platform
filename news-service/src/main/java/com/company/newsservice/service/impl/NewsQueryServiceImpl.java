package com.company.newsservice.service.impl;

import com.company.newsservice.domain.NewsArticle;
import com.company.newsservice.domain.enums.NewsCategory;
import com.company.newsservice.dto.NewsDetailResponse;
import com.company.newsservice.dto.NewsResponse;
import com.company.newsservice.exception.ResourceNotFoundException;
import com.company.newsservice.repository.NewsArticleRepository;
import com.company.newsservice.service.NewsQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsQueryServiceImpl implements NewsQueryService {

    private final NewsArticleRepository newsArticleRepository;

    @Override
    public Page<NewsResponse> search(NewsCategory category, String q, Pageable pageable) {
        return newsArticleRepository.search(category, normalize(q), pageable)
                .map(this::toResponse);
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

    private NewsResponse toResponse(NewsArticle article) {
        return new NewsResponse(
                article.getId(),
                article.getTitle(),
                article.getSummary(),
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