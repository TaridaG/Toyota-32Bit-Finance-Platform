package com.company.finance_api.portfolio.application;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.User;
import com.company.finance_api.domain.enums.TransactionType;
import com.company.finance_api.dto.PortfolioTradeFlowPointResponse;
import com.company.finance_api.dto.PortfolioTradeFlowResponse;
import com.company.finance_api.portfolio.InstrumentListingCurrency;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.repository.UserRepository;
import com.company.finance_api.pricing.application.CurrencyConversionService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** PortfolioTradeFlowServiceImpl iş mantığını uygular (portfolio trade flow service). */
@Service
public class PortfolioTradeFlowServiceImpl implements PortfolioTradeFlowService {

  private final TransactionRepository transactionRepository;
  private final UserRepository userRepository;
  private final ExternalPortfolioRepository externalPortfolioRepository;
  private final CurrentUserResolver currentUserResolver;
  private final CurrencyConversionService currencyConversionService;

  public PortfolioTradeFlowServiceImpl(
      TransactionRepository transactionRepository,
      UserRepository userRepository,
      ExternalPortfolioRepository externalPortfolioRepository,
      CurrentUserResolver currentUserResolver,
      CurrencyConversionService currencyConversionService) {
    this.transactionRepository = transactionRepository;
    this.userRepository = userRepository;
    this.externalPortfolioRepository = externalPortfolioRepository;
    this.currentUserResolver = currentUserResolver;
    this.currencyConversionService = currencyConversionService;
  }

  @Override
  @Transactional(readOnly = true)
  /** MyTradeFlow sorgusunu döner. */
  public PortfolioTradeFlowResponse getMyTradeFlow(String targetCurrency, Long portfolioId) {
    UUID userId = currentUserResolver.getCurrentUserId();
    String normalizedCurrency = currencyConversionService.normalizeCurrency(targetCurrency);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalStateException("User not found"));
    List<Transaction> txs;
    if (portfolioId == null) {
      txs = transactionRepository.findByUserOrderByCreatedAtAsc(user);
    } else {
      ExternalPortfolio portfolio =
          externalPortfolioRepository.findByIdAndUserId(portfolioId, userId).orElse(null);
      if (portfolio == null) {
        return new PortfolioTradeFlowResponse(normalizedCurrency, List.of());
      }
      txs =
          transactionRepository.findByUserAndExternalPortfolioOrderByCreatedAtAsc(user, portfolio);
    }
    List<PortfolioTradeFlowPointResponse> points = new ArrayList<>(txs.size());
    for (Transaction tx : txs) {
      Instrument ins = tx.getInstrument();
      BigDecimal notional = notionalInTarget(ins, tx.getTotalAmount(), normalizedCurrency);
      if (notional == null) {
        continue;
      }
      BigDecimal signed = tx.getType() == TransactionType.BUY ? notional : notional.negate();
      points.add(new PortfolioTradeFlowPointResponse(tx.getId(), tx.getCreatedAt(), signed));
    }
    points.sort(
        Comparator.comparing(PortfolioTradeFlowPointResponse::createdAt)
            .thenComparingLong(PortfolioTradeFlowPointResponse::transactionId));
    return new PortfolioTradeFlowResponse(normalizedCurrency, points);
  }

  /**
   * Uses {@code totalAmount} (price × quantity) in the instrument's quote currency, then converts
   * to the dashboard currency. This matches
   * {@link com.company.finance_api.portfolio.application.PortfolioOverviewServiceImpl} cost basis
   * logic
   * and avoids treating {@code inputAmount} (user wallet leg) as instrument notional when the two
   * diverge or were stored inconsistently.
   */
  private BigDecimal notionalInTarget(Instrument ins, BigDecimal totalAmount, String target) {
    if (totalAmount == null) {
      return null;
    }
    String normalizedTarget = currencyConversionService.normalizeCurrency(target);
    String insCur = InstrumentListingCurrency.resolve(ins);
    BigDecimal conv = currencyConversionService.convert(totalAmount, insCur, normalizedTarget);
    if (conv == null) {
      return null;
    }
    return conv.setScale(2, RoundingMode.HALF_UP);
  }
}
