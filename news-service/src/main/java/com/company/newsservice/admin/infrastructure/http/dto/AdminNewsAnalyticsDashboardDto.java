package com.company.newsservice.admin.infrastructure.http.dto;

import java.time.Instant;
import java.util.List;

/** Admin analytics dashboard response DTO'su. */
public record AdminNewsAnalyticsDashboardDto(
        String preset,
        Instant chartRangeStartUtcInclusive,
        Instant chartRangeEndUtcExclusive,
        AdminNewsAnalyticsSummaryDto summary,
        List<AdminNewsDailyPublishDto> dailyPublished,
        List<AdminRecentNewsArticleRowDto> recentArticles,
        Instant generatedAt
) {}
