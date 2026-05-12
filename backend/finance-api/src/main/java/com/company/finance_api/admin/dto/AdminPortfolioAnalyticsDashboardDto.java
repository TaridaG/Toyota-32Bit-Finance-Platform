package com.company.finance_api.admin.dto;

import java.time.Instant;
import java.util.List;

public record AdminPortfolioAnalyticsDashboardDto(
        String preset,
        Instant chartRangeStartUtcInclusive,
        Instant chartRangeEndUtcExclusive,
        AdminPortfolioAnalyticsSummaryDto summary,
        List<AdminPortfolioDailyCreationDto> dailyCreations,
        List<AdminRecentPortfolioRowDto> recentPortfolios,
        Instant generatedAt
) {}
