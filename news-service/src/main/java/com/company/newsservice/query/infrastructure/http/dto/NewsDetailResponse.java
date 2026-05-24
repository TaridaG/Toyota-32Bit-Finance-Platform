package com.company.newsservice.query.infrastructure.http.dto;

import com.company.newsservice.query.domain.enums.NewsCategory;

import java.time.Instant;
import java.util.List;

/**
 * Haber detay API response DTO'su (çeviri alanları ve audit timestamp'leri ile).
 */
public record NewsDetailResponse(
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
        NewsCategory category,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        List<String> relatedSymbols,
        List<String> topicTags
) {
}
