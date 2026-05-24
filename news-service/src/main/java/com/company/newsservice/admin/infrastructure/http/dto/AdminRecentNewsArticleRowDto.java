package com.company.newsservice.admin.infrastructure.http.dto;

import java.time.Instant;

/**
 * Son publish edilen article satırı (admin listesi için).
 */
public record AdminRecentNewsArticleRowDto(
        long id,
        String title,
        String sourceName,
        String category,
        Instant publishedAt
) {}
