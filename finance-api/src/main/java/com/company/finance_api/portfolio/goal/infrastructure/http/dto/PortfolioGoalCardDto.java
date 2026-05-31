package com.company.finance_api.portfolio.goal.infrastructure.http.dto;

import java.math.BigDecimal;

/** Tek bir hedef kartının ilerleme ve hedef detaylarını taşıyan DTO. */
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
    boolean configured) {}
