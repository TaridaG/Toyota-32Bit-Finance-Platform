package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AlarmHistoryResponse(
        String instrumentSymbol,
        String condition,
        BigDecimal price,
        Instant triggeredAt
) {}