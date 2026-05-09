package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PortfolioTradeFlowPointResponse(
        long transactionId,
        Instant createdAt,
        BigDecimal signedAmount
) {}
