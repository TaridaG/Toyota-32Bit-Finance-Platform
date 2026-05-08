package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionHistoryResponse(
        Long transactionId,
        Long portfolioId,
        String portfolioName,
        String instrumentSymbol,
        String type,
        String purchaseMode,
        String sourceLabel,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal totalAmount,
        String inputCurrency,
        BigDecimal inputAmount,
        BigDecimal fxRateUsed,
        Instant acquiredAt,
        Instant createdAt
) {}