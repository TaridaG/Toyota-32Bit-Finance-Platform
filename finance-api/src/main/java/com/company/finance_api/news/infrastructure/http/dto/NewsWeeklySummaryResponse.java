package com.company.finance_api.news.infrastructure.http.dto;

import java.util.List;

public record NewsWeeklySummaryResponse(
    long totalCount,
    List<NewsWeeklyTopicRowResponse> topics,
    List<NewsWeeklyAssetRowResponse> topAssets,
    List<NewsWeeklySourceRowResponse> sources,
    long portfolioRelatedCount) {}
