package com.company.finance_api.portfolio.application;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.portfolio.domain.InstrumentListingCurrency;
import com.company.finance_api.portfolio.domain.PositionCostBasisCalculator;
import com.company.finance_api.portfolio.domain.PositionCostBasisCalculator.PositionCostBasis;
import com.company.finance_api.portfolio.domain.Transaction;
import com.company.finance_api.portfolio.domain.enums.TransactionType;
import com.company.finance_api.portfolio.external.domain.ExternalPortfolio;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.infrastructure.http.dto.SalesAnalysisPageResponse;
import com.company.finance_api.portfolio.infrastructure.http.dto.SalesAnalysisRowResponse;
import com.company.finance_api.portfolio.infrastructure.persistence.TransactionRepository;
import com.company.finance_api.pricing.application.PriceService;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.profile.infrastructure.persistence.UserRepository;
import com.company.finance_api.shared.security.CurrentUserResolver;
import jakarta.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** Transaction ledger ve canlı mark fiyatlarından realized sell analytics hesaplar. */
@Service
@RequiredArgsConstructor
public class SalesAnalysisServiceImpl implements SalesAnalysisService {

  private static final int SCALE = 6;
  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private final TransactionRepository transactionRepository;
  private final CurrentUserResolver currentUserResolver;
  private final UserRepository userRepository;
  private final ExternalPortfolioRepository externalPortfolioRepository;
  private final PositionCostBasisCalculator positionCostBasisCalculator;
  private final PriceService priceService;

  @Override
  public SalesAnalysisPageResponse getMySalesAnalysisPage(
      int page,
      int size,
      Long portfolioId,
      String symbol,
      LocalDate fromDate,
      LocalDate toDate) {
    User user = resolveCurrentUser();
    if (portfolioId != null
        && externalPortfolioRepository.findByIdAndUserId(portfolioId, user.getId()).isEmpty()) {
      return new SalesAnalysisPageResponse(
          List.of(), Math.max(page, 0), Math.max(size, 1), 0, 0);
    }

    var pageable =
        PageRequest.of(
            Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"));

    Specification<Transaction> spec = sellSpec(user, portfolioId, symbol, fromDate, toDate);
    var result = transactionRepository.findAll(spec, pageable);

    Map<String, List<Transaction>> ledgerCache = new HashMap<>();
    List<SalesAnalysisRowResponse> rows =
        result.getContent().stream()
            .map(sell -> toRow(sell, ledgerCache))
            .toList();

    return new SalesAnalysisPageResponse(
        rows,
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages());
  }

  private SalesAnalysisRowResponse toRow(Transaction sell, Map<String, List<Transaction>> ledgerCache) {
    User user = sell.getUser();
    Instrument instrument = sell.getInstrument();
    ExternalPortfolio portfolio = sell.getExternalPortfolio();
    String cacheKey = ledgerKey(user.getId(), instrument.getId(), portfolio);

    List<Transaction> ledger =
        ledgerCache.computeIfAbsent(
            cacheKey,
            k ->
                transactionRepository
                    .findByUserAndInstrumentAndExternalPortfolio(user, instrument, portfolio)
                    .stream()
                    .sorted(
                        Comparator.comparing(SalesAnalysisServiceImpl::effectiveInstant)
                            .thenComparing(Transaction::getId))
                    .toList());

    List<Transaction> prior =
        ledger.stream().filter(tx -> isStrictlyBefore(tx, sell)).toList();
    PositionCostBasis basis = positionCostBasisCalculator.calculate(prior);

    BigDecimal qty = sell.getQuantity();
    BigDecimal avgCostAtSell = basis.averageCost();
    BigDecimal sellUnitPrice = sell.getPrice();
    BigDecimal sellProceeds = sellUnitPrice.multiply(qty);
    BigDecimal costBasis = avgCostAtSell.multiply(qty);
    BigDecimal realizedPnl = sellProceeds.subtract(costBasis);
    BigDecimal realizedPnlPct =
        costBasis.compareTo(BigDecimal.ZERO) > 0
            ? realizedPnl.multiply(HUNDRED).divide(costBasis, SCALE, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    BigDecimal currentUnitPrice =
        priceService.getLatestValuationPrice(instrument).map(p -> p.getPrice()).orElse(null);
    BigDecimal hypotheticalValueNow =
        currentUnitPrice != null ? currentUnitPrice.multiply(qty) : null;
    BigDecimal opportunityDelta =
        hypotheticalValueNow != null ? hypotheticalValueNow.subtract(sellProceeds) : null;

    return new SalesAnalysisRowResponse(
        sell.getId(),
        portfolio == null ? null : portfolio.getId(),
        portfolio == null ? null : portfolio.getName(),
        instrument.getId(),
        instrument.getSymbol(),
        qty,
        avgCostAtSell,
        sellUnitPrice,
        sellProceeds,
        costBasis,
        realizedPnl,
        realizedPnlPct,
        currentUnitPrice,
        hypotheticalValueNow,
        opportunityDelta,
        InstrumentListingCurrency.resolve(instrument),
        effectiveInstant(sell));
  }

  private static String ledgerKey(UUID userId, Long instrumentId, ExternalPortfolio portfolio) {
    Long portfolioId = portfolio == null ? null : portfolio.getId();
    return userId + "|" + instrumentId + "|" + portfolioId;
  }

  private static boolean isStrictlyBefore(Transaction tx, Transaction sell) {
    Instant txAt = effectiveInstant(tx);
    Instant sellAt = effectiveInstant(sell);
    int cmp = txAt.compareTo(sellAt);
    if (cmp < 0) {
      return true;
    }
    if (cmp > 0) {
      return false;
    }
    return tx.getId() < sell.getId();
  }

  private static Instant effectiveInstant(Transaction tx) {
    Instant acquired = tx.getAcquiredAt();
    return acquired != null ? acquired : tx.getCreatedAt();
  }

  private Specification<Transaction> sellSpec(
      User user,
      Long portfolioId,
      String symbol,
      LocalDate fromDate,
      LocalDate toDate) {
    return (root, query, cb) -> {
      if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
        root.fetch("instrument", JoinType.LEFT);
        query.distinct(true);
      }
      var predicate = cb.equal(root.get("user"), user);
      predicate = cb.and(predicate, cb.equal(root.get("type"), TransactionType.SELL));
      if (portfolioId != null) {
        predicate =
            cb.and(
                predicate,
                cb.equal(root.join("externalPortfolio", JoinType.LEFT).get("id"), portfolioId));
      }
      if (symbol != null && !symbol.isBlank()) {
        predicate =
            cb.and(
                predicate,
                cb.like(
                    cb.lower(root.join("instrument", JoinType.LEFT).get("symbol")),
                    "%" + symbol.toLowerCase(Locale.ROOT) + "%"));
      }
      if (fromDate != null) {
        Instant from = fromDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
      }
      if (toDate != null) {
        Instant to = toDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        predicate = cb.and(predicate, cb.lessThan(root.get("createdAt"), to));
      }
      return predicate;
    };
  }

  private User resolveCurrentUser() {
    UUID userId = currentUserResolver.getCurrentUserId();
    return userRepository.findById(userId).orElseThrow();
  }
}
