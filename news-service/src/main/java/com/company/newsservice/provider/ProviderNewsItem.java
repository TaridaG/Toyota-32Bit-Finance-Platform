package com.company.newsservice.provider;

import com.company.newsservice.domain.enums.NewsCategory;

import java.time.Instant;

public record ProviderNewsItem(
        String externalId,
        String title,
        String summary,
        String articleUrl,
        String sourceName,
        NewsCategory category,
        Instant publishedAt
) {
}