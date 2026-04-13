package com.company.analytics.application;

import com.company.analytics.event.AnalyticsMarketPriceEvent;

public interface RSIService {

    void process(AnalyticsMarketPriceEvent event);

}