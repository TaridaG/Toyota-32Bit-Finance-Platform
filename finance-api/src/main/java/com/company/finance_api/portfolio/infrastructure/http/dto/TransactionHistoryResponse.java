package com.company.finance_api.portfolio.infrastructure.http.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** TransactionHistoryResponse — API transfer nesnesi (DTO/response/request). */
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
    Instant createdAt,
    /** ISO currency for {@code price} / {@code totalAmount} (listing currency). */
    String quoteCurrency,
    /** Display leg base currency (e.g. USD for "1 USD = X TRY"). */
    String fxDisplayFrom,
    /** Display leg quote currency. */
    String fxDisplayTo,
    /** Units of {@code fxDisplayTo} per 1 {@code fxDisplayFrom}. */
    BigDecimal fxDisplayRate) {}
