package com.company.finance_api.domain.enums;

public enum OutboxStatus {
    NEW,
    RETRY,
    SENT,
    DEAD
}