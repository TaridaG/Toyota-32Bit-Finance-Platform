package com.company.newsservice.dto;

import com.company.newsservice.domain.enums.NewsCategory;

import java.time.Instant;
import java.util.List;

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