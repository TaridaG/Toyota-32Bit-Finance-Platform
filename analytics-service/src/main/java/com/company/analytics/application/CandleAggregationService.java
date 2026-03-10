package com.company.analytics.application;

import com.company.analytics.event.TransactionExecutedEvent;

public interface CandleAggregationService {

    void process(TransactionExecutedEvent event);
}