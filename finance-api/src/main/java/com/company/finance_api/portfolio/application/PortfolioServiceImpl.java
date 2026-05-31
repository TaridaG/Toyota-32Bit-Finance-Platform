package com.company.finance_api.portfolio.application;

import com.company.finance_api.domain.*;
import com.company.finance_api.dto.PortfolioPositionResponse;
import com.company.finance_api.dto.PortfolioSummaryResponse;
import com.company.finance_api.portfolio.domain.PortfolioPosition;
import com.company.finance_api.portfolio.domain.PortfolioPositionBuilder;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** PortfolioServiceImpl iş mantığını uygular (portfolio service). */
@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

  private final TransactionRepository transactionRepository;
  private final CurrentUserResolver currentUserResolver;
  private final UserRepository userRepository;
  private final PortfolioPositionBuilder portfolioPositionBuilder;

  /** MyPortfolio sorgusunu döner. */
  @Override
  public List<PortfolioPositionResponse> getMyPortfolio() {
    UUID userId = currentUserResolver.getCurrentUserId();
    return getPortfolioByUserId(userId);
  }

  private List<PortfolioPositionResponse> getPortfolioByUserId(UUID userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found"));
    List<Transaction> transactions = transactionRepository.findByUserOrderByCreatedAtDesc(user);
    Map<Instrument, List<Transaction>> byInstrument =
        transactions.stream().collect(Collectors.groupingBy(Transaction::getInstrument));
    List<PortfolioPositionResponse> positions = new ArrayList<>();
    for (Map.Entry<Instrument, List<Transaction>> entry : byInstrument.entrySet()) {
      Instrument instrument = entry.getKey();
      List<Transaction> txs = entry.getValue();
      Optional<PortfolioPosition> positionOpt = portfolioPositionBuilder.build(instrument, txs);
      if (positionOpt.isEmpty()) {
        continue;
      }
      PortfolioPosition position = positionOpt.get();
      positions.add(
          new PortfolioPositionResponse(
              position.instrument().getId(),
              position.instrument().getSymbol(),
              position.quantity(),
              position.averageCost(),
              position.currentPrice(),
              position.totalCost(),
              position.currentValue(),
              position.unrealizedPnl()));
    }
    return positions;
  }

  /** PortfolioSummary sorgusunu döner. */
  @Override
  public PortfolioSummaryResponse getPortfolioSummary() {
    UUID userId = currentUserResolver.getCurrentUserId();
    return getPortfolioSummary(userId);
  }

  /** PortfolioSummary sorgusunu döner. */
  @Override
  public PortfolioSummaryResponse getPortfolioSummary(UUID userId) {
    List<PortfolioPositionResponse> positions = getPortfolioByUserId(userId);
    BigDecimal totalCost = BigDecimal.ZERO;
    BigDecimal totalValue = BigDecimal.ZERO;
    for (PortfolioPositionResponse p : positions) {
      totalCost = totalCost.add(p.totalCost());
      totalValue = totalValue.add(p.currentValue());
    }
    BigDecimal unrealizedPnl = totalValue.subtract(totalCost);
    BigDecimal unrealizedPnlPercentage = BigDecimal.ZERO;
    if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
      unrealizedPnlPercentage =
          unrealizedPnl
              .divide(totalCost, 6, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(100));
    }
    return new PortfolioSummaryResponse(
        totalCost, totalValue, unrealizedPnl, unrealizedPnlPercentage);
  }
}
