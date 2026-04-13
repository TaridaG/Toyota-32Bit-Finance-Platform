package com.company.analytics.application;

import com.company.analytics.event.AnalyticsMarketPriceEvent;

public interface MovingAverageService {

    void process(AnalyticsMarketPriceEvent event);

}