package com.company.analytics.processing.application;

import com.company.analytics.processing.domain.event.AnalyticsMarketPriceEvent;

/** Günlük candle verilerinden RSI metriklerini hesaplayan servis sözleşmesi. */
public interface RSIService {

    /** Market price event'ine göre RSI14 değerini günceller. */
    void process(AnalyticsMarketPriceEvent event);

}