package com.company.finance_api.portfolio.goal;

import com.company.finance_api.common.ApiResponse;
import com.company.finance_api.portfolio.goal.dto.PortfolioGoalsViewResponse;
import com.company.finance_api.portfolio.goal.dto.UpsertPortfolioValueGoalRequest;
import com.company.finance_api.portfolio.goal.dto.UpsertProfitGoalRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio/goals")
public class PortfolioGoalController {

    private final PortfolioGoalService portfolioGoalService;

    public PortfolioGoalController(PortfolioGoalService portfolioGoalService) {
        this.portfolioGoalService = portfolioGoalService;
    }

    @GetMapping
    public ApiResponse<PortfolioGoalsViewResponse> getGoals(
            @RequestParam(value = "portfolioId", required = false) Long portfolioId,
            @RequestHeader(value = "X-Currency", required = false) String currency
    ) {
        return ApiResponse.success(portfolioGoalService.getGoals(portfolioId, currency));
    }

    @PutMapping("/portfolio-value")
    public ApiResponse<PortfolioGoalsViewResponse> upsertPortfolioValue(
            @Valid @RequestBody UpsertPortfolioValueGoalRequest request,
            @RequestHeader(value = "X-Currency", required = false) String currency
    ) {
        return ApiResponse.success(portfolioGoalService.upsertPortfolioValueGoal(request, currency));
    }

    @PutMapping("/profit")
    public ApiResponse<PortfolioGoalsViewResponse> upsertProfit(
            @Valid @RequestBody UpsertProfitGoalRequest request,
            @RequestHeader(value = "X-Currency", required = false) String currency
    ) {
        return ApiResponse.success(portfolioGoalService.upsertProfitGoal(request, currency));
    }
}
