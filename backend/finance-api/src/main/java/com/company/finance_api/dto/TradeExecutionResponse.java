package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeExecutionResponse(
        Long transactionId,
        Long instrumentId,
        String instrumentSymbol,
        String type,
        String purchaseMode,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        String inputCurrency,
        BigDecimal inputAmount,
        BigDecimal fxRateUsed,
        Instant acquiredAt,
        Instant createdAt
) {
}

