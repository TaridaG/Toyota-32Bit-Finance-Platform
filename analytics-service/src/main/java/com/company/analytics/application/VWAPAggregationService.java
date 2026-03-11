package com.company.analytics.application;

import com.company.analytics.event.TransactionExecutedEvent;

public interface VWAPAggregationService {

    void process(TransactionExecutedEvent event);

}