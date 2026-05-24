package com.company.analytics.processing.application;

/** Fiyat tipine göre hangi analytics pipeline adımlarının çalıştırılacağını belirten karar kaydı. */
public record AnalyticsProcessingDecision(
        boolean processCandle,
        boolean processMovingAverage,
        boolean processRsi,
        boolean processTrend
) {
}
