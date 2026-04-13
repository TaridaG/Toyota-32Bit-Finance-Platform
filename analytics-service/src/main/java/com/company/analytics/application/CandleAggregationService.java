package com.company.analytics.application;

import com.company.analytics.event.AnalyticsMarketPriceEvent;

public interface CandleAggregationService {

    void process(AnalyticsMarketPriceEvent event);
}