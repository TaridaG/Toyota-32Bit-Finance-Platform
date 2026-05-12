package com.company.newsservice.dto;

import java.time.Instant;

public record AdminRecentNewsArticleRowDto(
        long id,
        String title,
        String sourceName,
        String category,
        Instant publishedAt
) {}
