package com.company.finance_api.portfolio.infrastructure.http.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** TradeExecutionResponse — API transfer nesnesi (DTO/response/request). */
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
    Instant createdAt) {}
