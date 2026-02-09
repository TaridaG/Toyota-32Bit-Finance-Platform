package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionHistoryResponse(
        Long transactionId,
        String instrumentSymbol,
        String type,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        Instant createdAt
) {}