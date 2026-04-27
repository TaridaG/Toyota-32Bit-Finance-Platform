package com.company.analytics.application;

public record AnalyticsProcessingDecision(
        boolean processCandle,
        boolean processMovingAverage,
        boolean processRsi,
        boolean processTrend
) {
}
