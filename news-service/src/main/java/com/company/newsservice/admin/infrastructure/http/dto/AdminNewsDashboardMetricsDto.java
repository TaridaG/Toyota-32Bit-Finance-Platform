package com.company.newsservice.admin.infrastructure.http.dto;

import java.time.Instant;
import java.util.List;

/** Admin dashboard: article volume ve source breadth (UTC day bucket'ları). */
public record AdminNewsDashboardMetricsDto(
        long totalArticles,
        int distinctSourceCount,
        List<Integer> articlesPublishedDailyLast7Utc,
        Instant generatedAt
) {}
