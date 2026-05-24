package com.company.newsservice.ingestion.domain;

import com.company.newsservice.query.domain.enums.NewsCategory;

import java.time.Instant;

/**
 * Provider'dan gelen tek bir haber öğesinin ingestion pipeline'ına taşınan ham temsilidir.
 */
public record ProviderNewsItem(
        String externalId,
        String title,
        String summary,
        String articleUrl,
        String sourceName,
        NewsCategory category,
        Instant publishedAt,
        String rssImageUrl
) {
    public ProviderNewsItem(
            String externalId,
            String title,
            String summary,
            String articleUrl,
            String sourceName,
            NewsCategory category,
            Instant publishedAt
    ) {
        this(externalId, title, summary, articleUrl, sourceName, category, publishedAt, null);
    }
}