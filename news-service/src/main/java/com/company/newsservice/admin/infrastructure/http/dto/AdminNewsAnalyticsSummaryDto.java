package com.company.newsservice.admin.infrastructure.http.dto;

/** Analytics özet metrikleri (roster, translation, publish trend). */
public record AdminNewsAnalyticsSummaryDto(
        long totalArticles,
        int distinctSourceCount,
        double translationCompletionPercent,
        String translationMeasuredLanguage,
        long articlesPublishedPreviousIsoWeekUtc,
        long articlesPublishedPreviousCalendarMonthUtc,
        long articlesCreatedYesterdayUtc,
        long articlesCreatedDayBeforeYesterdayUtc,
        /** Dünkü ingest'e göre implied prior total ile yaklaşık roster day-over-day yüzde değişimi. */
        double totalArticlesVsPriorDayPercentApprox
) {}
