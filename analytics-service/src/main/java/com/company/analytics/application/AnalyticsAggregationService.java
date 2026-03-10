package com.company.analytics.application;

import com.company.analytics.event.TransactionExecutedEvent;

public interface AnalyticsAggregationService {

    void process(TransactionExecutedEvent event);

}