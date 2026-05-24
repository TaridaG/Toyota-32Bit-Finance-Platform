package com.company.analytics.processing.application;

import com.company.analytics.processing.domain.event.AnalyticsMarketPriceEvent;

/** Moving average ve günlük candle verilerinden trend metriklerini hesaplayan servis sözleşmesi. */
public interface TrendMetricService {

    /** Market price event'ine göre trend direction, momentum ve slope değerlerini günceller. */
    void process(AnalyticsMarketPriceEvent event);
}