package com.company.newsservice.dto;

import com.company.newsservice.domain.enums.NewsCategory;

import java.time.Instant;
import java.util.List;

public record NewsDetailResponse(
        Long id,
        String title,
        String summary,
        String titleOriginal,
        String summaryOriginal,
        String translatedLanguage,
        boolean translated,
        String articleUrl,
        String sourceName,
        NewsCategory category,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt,
        List<String> relatedSymbols,
        List<String> topicTags
) {
}
