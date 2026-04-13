package com.company.analytics.application;

import com.company.analytics.event.AnalyticsMarketPriceEvent;

public interface TrendMetricService {

    void process(AnalyticsMarketPriceEvent event);
}