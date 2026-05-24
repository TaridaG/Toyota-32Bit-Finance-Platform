package com.company.analytics.processing.application;

import com.company.analytics.processing.domain.event.AnalyticsMarketPriceEvent;

/** Günlük candle verilerinden moving average metriklerini hesaplayan servis sözleşmesi. */
public interface MovingAverageService {

    /** Market price event'ine göre MA7, MA30 ve MA90 değerlerini günceller. */
    void process(AnalyticsMarketPriceEvent event);

}