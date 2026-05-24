package com.company.newsservice.query.infrastructure.http.dto;

import com.company.newsservice.query.domain.enums.NewsCategory;

import java.time.Instant;
import java.util.List;

/**
 * Haber liste/chart API response DTO'su (çeviri alanları ve metadata ile).
 */
public record NewsResponse(
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
        List<String> relatedSymbols,
        List<String> topicTags
) {
}