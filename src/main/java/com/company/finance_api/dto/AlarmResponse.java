package com.company.finance_api.dto;

import com.company.finance_api.domain.enums.AlarmCondition;

import java.math.BigDecimal;
import java.time.Instant;

public class AlarmResponse {

    private final Long id;
    private final String instrumentSymbol;
    private final AlarmCondition condition;
    private final BigDecimal threshold;
    private final boolean active;
    private final Instant createdAt;

    public AlarmResponse(
            Long id,
            String instrumentSymbol,
            AlarmCondition condition,
            BigDecimal threshold,
            boolean active,
            Instant createdAt
    ) {
        this.id = id;
        this.instrumentSymbol = instrumentSymbol;
        this.condition = condition;
        this.threshold = threshold;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getInstrumentSymbol() { return instrumentSymbol; }
    public AlarmCondition getCondition() { return condition; }
    public BigDecimal getThreshold() { return threshold; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
