package com.company.newsservice.dto;

import java.time.Instant;
import java.util.List;

public record AdminNewsAnalyticsDashboardDto(
        String preset,
        Instant chartRangeStartUtcInclusive,
        Instant chartRangeEndUtcExclusive,
        AdminNewsAnalyticsSummaryDto summary,
        List<AdminNewsDailyPublishDto> dailyPublished,
        List<AdminRecentNewsArticleRowDto> recentArticles,
        Instant generatedAt
) {}
