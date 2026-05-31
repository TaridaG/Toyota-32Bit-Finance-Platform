package com.company.finance_api.infocards.infrastructure.http.dto;

/** Admin info-card dashboard özet metrikleri. */
public record InfoCardsDashboardDto(
    long activeCards,
    long passiveCards,
    double averageWordCount,
    long coveredPages,
    String mostCoveredPage,
    long beginnerCount,
    long intermediateCount,
    long advancedCount,
    String lastUpdatedCardTitle) {}
