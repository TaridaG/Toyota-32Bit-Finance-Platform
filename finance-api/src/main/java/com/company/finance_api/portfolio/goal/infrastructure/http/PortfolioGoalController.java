package com.company.finance_api.portfolio.goal.infrastructure.http;

import com.company.finance_api.portfolio.goal.PortfolioGoalService;
import com.company.finance_api.portfolio.goal.dto.PortfolioGoalsViewResponse;
import com.company.finance_api.portfolio.goal.dto.UpsertPortfolioValueGoalRequest;
import com.company.finance_api.portfolio.goal.dto.UpsertProfitGoalRequest;
import com.company.finance_api.shared.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Portfolio hedefleri için REST endpoint'leri sağlayan controller. */
@RestController
@RequestMapping("/api/portfolio/goals")
public class PortfolioGoalController {

  private final PortfolioGoalService portfolioGoalService;

  /** PortfolioGoalService bağımlılığını enjekte eder. */
  public PortfolioGoalController(PortfolioGoalService portfolioGoalService) {
    this.portfolioGoalService = portfolioGoalService;
  }

  /** Kullanıcının portfolio hedef görünümünü getirir. */
  @GetMapping
  public ApiResponse<PortfolioGoalsViewResponse> getGoals(
      @RequestParam(value = "portfolioId", required = false) Long portfolioId,
      @RequestHeader(value = "X-Currency", required = false) String currency) {
    return ApiResponse.success(portfolioGoalService.getGoals(portfolioId, currency));
  }

  /** Portfolio değer hedefini oluşturur veya günceller. */
  @PutMapping("/portfolio-value")
  public ApiResponse<PortfolioGoalsViewResponse> upsertPortfolioValue(
      @Valid @RequestBody UpsertPortfolioValueGoalRequest request,
      @RequestHeader(value = "X-Currency", required = false) String currency) {
    return ApiResponse.success(portfolioGoalService.upsertPortfolioValueGoal(request, currency));
  }

  /** Kar hedefini oluşturur veya günceller. */
  @PutMapping("/profit")
  public ApiResponse<PortfolioGoalsViewResponse> upsertProfit(
      @Valid @RequestBody UpsertProfitGoalRequest request,
      @RequestHeader(value = "X-Currency", required = false) String currency) {
    return ApiResponse.success(portfolioGoalService.upsertProfitGoal(request, currency));
  }
}
