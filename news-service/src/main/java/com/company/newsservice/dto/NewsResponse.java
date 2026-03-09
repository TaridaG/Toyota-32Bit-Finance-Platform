package com.company.newsservice.dto;

import com.company.newsservice.domain.enums.NewsCategory;

import java.time.Instant;

public record NewsResponse(
        Long id,
        String title,
        String summary,
        String articleUrl,
        String sourceName,
        NewsCategory category,
        Instant publishedAt
) {
}