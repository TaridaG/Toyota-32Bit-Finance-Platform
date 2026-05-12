package com.company.newsservice.dto;

import java.time.Instant;
import java.util.List;

/** Admin dashboard: article volume and source breadth (UTC day buckets). */
public record AdminNewsDashboardMetricsDto(
        long totalArticles,
        int distinctSourceCount,
        List<Integer> articlesPublishedDailyLast7Utc,
        Instant generatedAt
) {}
