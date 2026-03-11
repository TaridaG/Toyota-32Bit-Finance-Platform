package com.company.analytics.application;

import com.company.analytics.event.TransactionExecutedEvent;

public interface MovingAverageService {

    void process(TransactionExecutedEvent event);

}