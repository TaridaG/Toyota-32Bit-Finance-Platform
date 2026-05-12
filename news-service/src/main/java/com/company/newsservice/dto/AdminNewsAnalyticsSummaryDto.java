package com.company.newsservice.dto;

public record AdminNewsAnalyticsSummaryDto(
        long totalArticles,
        int distinctSourceCount,
        double translationCompletionPercent,
        String translationMeasuredLanguage,
        long articlesPublishedPreviousIsoWeekUtc,
        long articlesPublishedPreviousCalendarMonthUtc,
        long articlesCreatedYesterdayUtc,
        long articlesCreatedDayBeforeYesterdayUtc,
        /** Approximate roster day-over-day % from yesterday ingest vs implied prior total. */
        double totalArticlesVsPriorDayPercentApprox
) {}
