package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class BalanceResponse {

    private UUID userId;
    private BigDecimal demoBalance;

    public BalanceResponse(UUID userId, BigDecimal demoBalance) {
        this.userId = userId;
        this.demoBalance = demoBalance;
    }

    public UUID getUserId() {
        return userId;
    }

    public BigDecimal getDemoBalance() {
        return demoBalance;
    }
}
