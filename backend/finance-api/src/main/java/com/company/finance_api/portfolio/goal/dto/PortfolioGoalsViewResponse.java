package com.company.finance_api.portfolio.goal.dto;

public record PortfolioGoalsViewResponse(
        String scope,
        Long portfolioId,
        String currency,
        PortfolioGoalCardDto portfolioValueGoal,
        PortfolioGoalCardDto profitGoal
) {
}
