package com.company.analytics.application;

import com.company.analytics.event.TransactionExecutedEvent;

public interface TrendMetricService {

    void process(TransactionExecutedEvent event);
}