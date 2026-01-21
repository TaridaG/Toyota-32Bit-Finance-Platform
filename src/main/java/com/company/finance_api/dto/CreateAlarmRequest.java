package com.company.finance_api.dto;

import com.company.finance_api.domain.enums.AlarmCondition;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateAlarmRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private long instrumentId;

    @Getter
    @NotNull
    private AlarmCondition condition;

    @Getter
    @NotNull
    private BigDecimal threshold;

    public Long getInstrumentId() {
        return instrumentId;
    }

}
