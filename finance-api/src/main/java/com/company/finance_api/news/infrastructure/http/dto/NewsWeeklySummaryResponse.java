package com.company.finance_api.news.infrastructure.http.dto;

import java.util.List;

/** NewsWeeklySummaryResponse — API transfer nesnesi (DTO/response/request). */
public record NewsWeeklySummaryResponse(
    long totalCount,
    List<NewsWeeklyTopicRowResponse> topics,
    List<NewsWeeklyAssetRowResponse> topAssets,
    List<NewsWeeklySourceRowResponse> sources,
    long portfolioRelatedCount) {}
