package com.company.finance_api.event;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class AlarmTriggeredEvent {

    private final Long alarmId;
    private final UUID userId;
    private final String instrumentSymbol;
    private final String condition;
    private final String price;
    private final Instant triggeredAt;

    public AlarmTriggeredEvent(
            Long alarmId,
            UUID userId,
            String instrumentSymbol,
            String condition,
            String price,
            Instant triggeredAt
    ) {
        this.alarmId = alarmId;
        this.userId = userId;
        this.instrumentSymbol = instrumentSymbol;
        this.condition = condition;
        this.price = price;
        this.triggeredAt = triggeredAt;
    }

}
