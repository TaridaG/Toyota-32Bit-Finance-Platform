package com.company.finance_api.portfolio.goal.dto;

import java.math.BigDecimal;

public record PortfolioGoalCardDto(
        String goalType,
        String profitTargetMode,
        BigDecimal targetAmount,
        BigDecimal targetPercent,
        String title,
        String description,
        BigDecimal currentAmount,
        BigDecimal currentPercent,
        BigDecimal progressRatio,
        String barVariant,
        boolean configured
) {
}
