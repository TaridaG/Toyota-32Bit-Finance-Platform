package com.company.finance_api.portfolio.goal.dto;

/** Portfolio hedef kartlarını ve kapsam bilgisini içeren görünüm response DTO'su. */
public record PortfolioGoalsViewResponse(
    String scope,
    Long portfolioId,
    String currency,
    PortfolioGoalCardDto portfolioValueGoal,
    PortfolioGoalCardDto profitGoal) {}
