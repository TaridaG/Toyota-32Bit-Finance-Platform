package com.company.finance_api.portfolio.infrastructure.http.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** PortfolioTradeFlowPointResponse — API transfer nesnesi (DTO/response/request). */
public record PortfolioTradeFlowPointResponse(
    long transactionId, Instant createdAt, BigDecimal signedAmount) {}
