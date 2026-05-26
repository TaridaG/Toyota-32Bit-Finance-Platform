package com.company.finance_api.portfolio.goal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.User;
import com.company.finance_api.dto.PortfolioOverviewResponse;
import com.company.finance_api.portfolio.application.PortfolioOverviewService;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.goal.dto.UpsertPortfolioValueGoalRequest;
import com.company.finance_api.portfolio.goal.dto.UpsertProfitGoalRequest;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PortfolioGoalServiceTest {

  @Mock private CurrentUserResolver currentUserResolver;
  @Mock private UserRepository userRepository;
  @Mock private ExternalPortfolioRepository externalPortfolioRepository;
  @Mock private PortfolioGoalRepository portfolioGoalRepository;
  @Mock private PortfolioOverviewService portfolioOverviewService;

  private PortfolioGoalService portfolioGoalService;

  private final UUID userId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    portfolioGoalService =
        new PortfolioGoalService(
            currentUserResolver,
            userRepository,
            externalPortfolioRepository,
            portfolioGoalRepository,
            portfolioOverviewService);
  }

  @Test
  void getGoals_buildsUserScopeView() {
    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(portfolioOverviewService.getMyOverview("USD", null))
        .thenReturn(overview("USD", "1000", "100", "10"));
    when(portfolioGoalRepository.findForScopeAndType(userId, null, GoalType.PORTFOLIO_VALUE))
        .thenReturn(Optional.empty());
    when(portfolioGoalRepository.findForScopeAndType(userId, null, GoalType.PROFIT))
        .thenReturn(Optional.empty());

    var view = portfolioGoalService.getGoals(null, "USD");

    assertEquals("USER", view.scope());
    assertEquals("USD", view.currency());
    assertEquals(false, view.portfolioValueGoal().configured());
  }

  @Test
  void upsertPortfolioValueGoal_persistsGoal() {
    User user = new User("user@example.com", "trader");
    ExternalPortfolio portfolio = new ExternalPortfolio(user, "Growth", "USD");
    UpsertPortfolioValueGoalRequest request = new UpsertPortfolioValueGoalRequest();
    request.setPortfolioId(5L);
    request.setTargetAmount(new BigDecimal("25000"));
    request.setTitle("Retirement");

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(externalPortfolioRepository.findByIdAndUserId(5L, userId))
        .thenReturn(Optional.of(portfolio));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    when(portfolioGoalRepository.findForScopeAndType(userId, 5L, GoalType.PORTFOLIO_VALUE))
        .thenReturn(Optional.empty());
    when(portfolioOverviewService.getMyOverview("USD", 5L))
        .thenReturn(overview("USD", "5000", "50", "5"));
    when(portfolioGoalRepository.findForScopeAndType(userId, 5L, GoalType.PROFIT))
        .thenReturn(Optional.empty());

    var view = portfolioGoalService.upsertPortfolioValueGoal(request, "USD");

    verify(portfolioGoalRepository).save(any(PortfolioGoal.class));
    assertEquals("PORTFOLIO", view.scope());
    assertEquals(5L, view.portfolioId());
  }

  @Test
  void upsertProfitGoal_percentMode_requiresTargetPercent() {
    User user = new User("user@example.com", "trader");
    UpsertProfitGoalRequest request = new UpsertProfitGoalRequest();
    request.setPortfolioId(5L);
    request.setProfitTargetMode("PERCENT");

    when(currentUserResolver.getCurrentUserId()).thenReturn(userId);
    when(externalPortfolioRepository.findByIdAndUserId(5L, userId))
        .thenReturn(Optional.of(new ExternalPortfolio(user, "P", "USD")));
    when(userRepository.findById(userId)).thenReturn(Optional.of(user));

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> portfolioGoalService.upsertProfitGoal(request, "USD"));

    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  private static PortfolioOverviewResponse overview(
      String currency, String totalValue, String totalPnl, String totalPnlPercent) {
    return new PortfolioOverviewResponse(
        currency,
        new BigDecimal(totalValue),
        new BigDecimal("800"),
        new BigDecimal(totalPnl),
        new BigDecimal(totalPnlPercent),
        BigDecimal.ZERO,
        List.of());
  }
}
