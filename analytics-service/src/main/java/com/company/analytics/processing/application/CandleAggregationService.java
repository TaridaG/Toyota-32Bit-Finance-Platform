package com.company.analytics.processing.application;

import com.company.analytics.processing.domain.event.AnalyticsMarketPriceEvent;

/** Gelen market price event'lerinden candle agregasyonu yapan servis sözleşmesi. */
public interface CandleAggregationService {

    /** Market price event'ini alarak ilgili interval candle kayıtlarını günceller. */
    void process(AnalyticsMarketPriceEvent event);
}