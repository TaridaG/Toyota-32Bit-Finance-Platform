package com.company.finance_api.event.publisher;

import com.company.finance_api.event.TransactionExecutedEvent;

public interface TransactionEventPublisher {
    void publish(TransactionExecutedEvent event);
}