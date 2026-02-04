package com.company.finance_api.event;

import com.company.finance_api.domain.enums.AlarmCondition;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
public class AlarmTriggeredEvent {

    private final Long alarmId;
    private final UUID userId;
    private final String instrumentSymbol;
    private final AlarmCondition condition;
    private final BigDecimal price;
    private final Instant triggeredAt;

    private AlarmTriggeredEvent(
            Long alarmId,
            UUID userId,
            String instrumentSymbol,
            AlarmCondition condition,
            BigDecimal price,
            Instant triggeredAt
    ) {
        this.alarmId = alarmId;
        this.userId = userId;
        this.instrumentSymbol = instrumentSymbol;
        this.condition = condition;
        this.price = price;
        this.triggeredAt = triggeredAt;
    }

    // ✅ FACTORY METHOD (BEST PRACTICE)
    public static AlarmTriggeredEvent of(
            Long alarmId,
            UUID userId,
            String instrumentSymbol,
            AlarmCondition condition,
            BigDecimal price
    ) {
        return new AlarmTriggeredEvent(
                alarmId,
                userId,
                instrumentSymbol,
                condition,
                price,
                Instant.now()
        );
    }
}