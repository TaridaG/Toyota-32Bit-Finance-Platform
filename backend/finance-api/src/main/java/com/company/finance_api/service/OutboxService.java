package com.company.finance_api.service;

public interface OutboxService {
    void enqueue(String topic, String messageKey, Object payload);
}