package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** PortfolioTradeFlowPointResponse — API transfer nesnesi (DTO/response/request). */
public record PortfolioTradeFlowPointResponse(
    long transactionId, Instant createdAt, BigDecimal signedAmount) {}
