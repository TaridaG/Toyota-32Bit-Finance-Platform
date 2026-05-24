package com.company.finance_api.admin.infrastructure.http.dto;

import java.time.Instant;
import java.util.List;

/** Enterprise portfolio analytics dashboard payload'u (UTC). */
public record AdminPortfolioAnalyticsDashboardDto(
    String preset,
    Instant chartRangeStartUtcInclusive,
    Instant chartRangeEndUtcExclusive,
    AdminPortfolioAnalyticsSummaryDto summary,
    List<AdminPortfolioDailyCreationDto> dailyCreations,
    List<AdminRecentPortfolioRowDto> recentPortfolios,
    Instant generatedAt) {}
