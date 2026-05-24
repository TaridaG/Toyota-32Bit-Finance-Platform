package com.company.finance_api.portfolio.goal;

import com.company.finance_api.domain.User;
import com.company.finance_api.dto.PortfolioOverviewResponse;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.goal.dto.PortfolioGoalCardDto;
import com.company.finance_api.portfolio.goal.dto.PortfolioGoalsViewResponse;
import com.company.finance_api.portfolio.goal.dto.UpsertPortfolioValueGoalRequest;
import com.company.finance_api.portfolio.goal.dto.UpsertProfitGoalRequest;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.service.PortfolioOverviewService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Portfolio ve kar hedeflerinin okunması, oluşturulması ve güncellenmesi için application service.
 */
@Service
public class PortfolioGoalService {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final CurrentUserResolver currentUserResolver;
  private final UserRepository userRepository;
  private final ExternalPortfolioRepository externalPortfolioRepository;
  private final PortfolioGoalRepository portfolioGoalRepository;
  private final PortfolioOverviewService portfolioOverviewService;

  /** Gerekli bağımlılıkları enjekte eder. */
  public PortfolioGoalService(
      CurrentUserResolver currentUserResolver,
      UserRepository userRepository,
      ExternalPortfolioRepository externalPortfolioRepository,
      PortfolioGoalRepository portfolioGoalRepository,
      PortfolioOverviewService portfolioOverviewService) {
    this.currentUserResolver = currentUserResolver;
    this.userRepository = userRepository;
    this.externalPortfolioRepository = externalPortfolioRepository;
    this.portfolioGoalRepository = portfolioGoalRepository;
    this.portfolioOverviewService = portfolioOverviewService;
  }

  /** Kullanıcının belirtilen kapsam için portfolio hedef görünümünü döner. */
  @Transactional(readOnly = true)
  public PortfolioGoalsViewResponse getGoals(Long portfolioId, String targetCurrency) {
    UUID userId = currentUserResolver.getCurrentUserId();
    resolvePortfolioForUser(userId, portfolioId);
    return buildView(userId, portfolioId, targetCurrency);
  }

  /** Portfolio değer hedefini oluşturur veya günceller. */
  @Transactional
  public PortfolioGoalsViewResponse upsertPortfolioValueGoal(
      UpsertPortfolioValueGoalRequest request, String targetCurrency) {
    UUID userId = currentUserResolver.getCurrentUserId();
    Long portfolioId = request.getPortfolioId();
    ExternalPortfolio portfolio = resolvePortfolioForUser(userId, portfolioId);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    PortfolioGoal goal =
        portfolioGoalRepository
            .findForScopeAndType(userId, portfolioId, GoalType.PORTFOLIO_VALUE)
            .orElseGet(PortfolioGoal::new);
    goal.setUser(user);
    goal.setExternalPortfolio(portfolio);
    goal.setGoalType(GoalType.PORTFOLIO_VALUE);
    goal.setProfitTargetMode(null);
    goal.setTargetAmount(request.getTargetAmount().setScale(4, RoundingMode.HALF_UP));
    goal.setTargetPercent(null);
    goal.setTitle(normalizeTitle(request.getTitle(), "Portföy hedefi"));
    goal.setDescription(normalizeDescription(request.getDescription()));
    portfolioGoalRepository.save(goal);
    return buildView(userId, portfolioId, targetCurrency);
  }

  /** Kar hedefini (mutlak veya yüzde) oluşturur veya günceller. */
  @Transactional
  public PortfolioGoalsViewResponse upsertProfitGoal(
      UpsertProfitGoalRequest request, String targetCurrency) {
    UUID userId = currentUserResolver.getCurrentUserId();
    Long portfolioId = request.getPortfolioId();
    ExternalPortfolio portfolio = resolvePortfolioForUser(userId, portfolioId);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

    ProfitTargetMode mode = parseProfitMode(request.getProfitTargetMode());
    if (mode == ProfitTargetMode.PERCENT) {
      if (request.getTargetPercent() == null
          || request.getTargetPercent().compareTo(BigDecimal.ZERO) <= 0) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "targetPercent is required for percent profit goals");
      }
    } else if (request.getTargetAmount() == null
        || request.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "targetAmount is required for absolute profit goals");
    }

    PortfolioGoal goal =
        portfolioGoalRepository
            .findForScopeAndType(userId, portfolioId, GoalType.PROFIT)
            .orElseGet(PortfolioGoal::new);
    goal.setUser(user);
    goal.setExternalPortfolio(portfolio);
    goal.setGoalType(GoalType.PROFIT);
    goal.setProfitTargetMode(mode);
    goal.setTargetAmount(
        mode == ProfitTargetMode.ABSOLUTE
            ? request.getTargetAmount().setScale(4, RoundingMode.HALF_UP)
            : null);
    goal.setTargetPercent(
        mode == ProfitTargetMode.PERCENT
            ? request.getTargetPercent().setScale(4, RoundingMode.HALF_UP)
            : null);
    goal.setTitle(normalizeTitle(request.getTitle(), "Kar hedefi"));
    goal.setDescription(normalizeDescription(request.getDescription()));
    portfolioGoalRepository.save(goal);
    return buildView(userId, portfolioId, targetCurrency);
  }

  private PortfolioGoalsViewResponse buildView(
      UUID userId, Long portfolioId, String targetCurrency) {
    PortfolioOverviewResponse overview =
        portfolioOverviewService.getMyOverview(targetCurrency, portfolioId);
    String currency = overview.currency() != null ? overview.currency() : "USD";

    PortfolioGoal portfolioGoal =
        portfolioGoalRepository
            .findForScopeAndType(userId, portfolioId, GoalType.PORTFOLIO_VALUE)
            .orElse(null);
    PortfolioGoal profitGoal =
        portfolioGoalRepository
            .findForScopeAndType(userId, portfolioId, GoalType.PROFIT)
            .orElse(null);

    String scope = portfolioId == null ? "USER" : "PORTFOLIO";
    return new PortfolioGoalsViewResponse(
        scope,
        portfolioId,
        currency,
        toPortfolioValueCard(portfolioGoal, overview),
        toProfitCard(profitGoal, overview));
  }

  private PortfolioGoalCardDto toPortfolioValueCard(
      PortfolioGoal goal, PortfolioOverviewResponse overview) {
    BigDecimal current = safe(overview.totalValue());
    if (goal == null) {
      return emptyCard("PORTFOLIO_VALUE", current, null, "PORTFOLIO_TOWARD");
    }
    BigDecimal target = safe(goal.getTargetAmount());
    BigDecimal ratio =
        target.compareTo(BigDecimal.ZERO) > 0
            ? current.divide(target, 6, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    return new PortfolioGoalCardDto(
        GoalType.PORTFOLIO_VALUE.name(),
        null,
        target,
        null,
        goal.getTitle(),
        goal.getDescription(),
        current,
        null,
        ratio,
        "PORTFOLIO_TOWARD",
        true);
  }

  private PortfolioGoalCardDto toProfitCard(
      PortfolioGoal goal, PortfolioOverviewResponse overview) {
    BigDecimal pnl = safe(overview.totalPnl());
    BigDecimal pnlPct =
        overview.totalPnlPercent() != null ? overview.totalPnlPercent() : BigDecimal.ZERO;
    if (goal == null) {
      String variant = pnl.compareTo(BigDecimal.ZERO) < 0 ? "PROFIT_LOSS" : "PROFIT_GAIN";
      return emptyCard("PROFIT", pnl, pnlPct, variant);
    }

    BigDecimal ratio;
    String variant;
    if (goal.getProfitTargetMode() == ProfitTargetMode.PERCENT) {
      BigDecimal targetPct = safe(goal.getTargetPercent());
      if (pnl.compareTo(BigDecimal.ZERO) < 0) {
        variant = "PROFIT_LOSS";
        ratio =
            targetPct.compareTo(BigDecimal.ZERO) > 0
                ? pnlPct.abs().divide(targetPct, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
      } else {
        variant = "PROFIT_GAIN";
        ratio =
            targetPct.compareTo(BigDecimal.ZERO) > 0
                ? pnlPct.divide(targetPct, 6, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
      }
      return new PortfolioGoalCardDto(
          GoalType.PROFIT.name(),
          ProfitTargetMode.PERCENT.name(),
          null,
          targetPct,
          goal.getTitle(),
          goal.getDescription(),
          pnl,
          pnlPct,
          ratio,
          variant,
          true);
    }

    BigDecimal targetAmt = safe(goal.getTargetAmount());
    if (pnl.compareTo(BigDecimal.ZERO) < 0) {
      variant = "PROFIT_LOSS";
      ratio =
          targetAmt.compareTo(BigDecimal.ZERO) > 0
              ? pnl.abs().divide(targetAmt, 6, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;
    } else {
      variant = "PROFIT_GAIN";
      ratio =
          targetAmt.compareTo(BigDecimal.ZERO) > 0
              ? pnl.divide(targetAmt, 6, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;
    }
    return new PortfolioGoalCardDto(
        GoalType.PROFIT.name(),
        ProfitTargetMode.ABSOLUTE.name(),
        targetAmt,
        null,
        goal.getTitle(),
        goal.getDescription(),
        pnl,
        pnlPct,
        ratio,
        variant,
        true);
  }

  private static PortfolioGoalCardDto emptyCard(
      String goalType, BigDecimal currentAmount, BigDecimal currentPercent, String barVariant) {
    return new PortfolioGoalCardDto(
        goalType,
        null,
        null,
        null,
        null,
        null,
        currentAmount,
        currentPercent,
        BigDecimal.ZERO,
        barVariant,
        false);
  }

  private ExternalPortfolio resolvePortfolioForUser(UUID userId, Long portfolioId) {
    if (portfolioId == null) {
      return null;
    }
    return externalPortfolioRepository
        .findByIdAndUserId(portfolioId, userId)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Portfolio not found"));
  }

  private static ProfitTargetMode parseProfitMode(String raw) {
    if (!StringUtils.hasText(raw)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "profitTargetMode is required");
    }
    String normalized = raw.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "PERCENT", "PCT", "PERCENTAGE" -> ProfitTargetMode.PERCENT;
      case "ABSOLUTE", "AMOUNT" -> ProfitTargetMode.ABSOLUTE;
      default ->
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported profitTargetMode");
    };
  }

  private static String normalizeTitle(String raw, String fallback) {
    if (!StringUtils.hasText(raw)) {
      return fallback;
    }
    return raw.trim();
  }

  private static String normalizeDescription(String raw) {
    if (!StringUtils.hasText(raw)) {
      return null;
    }
    return raw.trim();
  }

  private static BigDecimal safe(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }
}
