package com.company.notification.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class AlarmTriggeredEvent {

    private Long alarmId;
    private UUID userId;
    private String instrumentSymbol;
    private String condition;
    private BigDecimal price;
    private Instant triggeredAt;

    public Long getAlarmId() { return alarmId; }
    public UUID getUserId() { return userId; }
    public String getInstrumentSymbol() { return instrumentSymbol; }
    public String getCondition() { return condition; }
    public BigDecimal getPrice() { return price; }
    public Instant getTriggeredAt() { return triggeredAt; }
}
