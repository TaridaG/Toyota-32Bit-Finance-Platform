package com.company.analytics.application;

import com.company.analytics.event.TransactionExecutedEvent;

public interface RSIService {

    void process(TransactionExecutedEvent event);

}