package com.company.finance_api.dto;

import com.company.finance_api.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeMarkerResponse(
        Instant time,
        TransactionType type,
        BigDecimal price,
        BigDecimal quantity
) {}