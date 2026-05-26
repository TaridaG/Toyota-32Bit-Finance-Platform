package com.company.finance_api.portfolio.application;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.PortfolioDailyPerformance;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.enums.TransactionType;
import com.company.finance_api.dto.PortfolioPerformancePointResponse;
import com.company.finance_api.dto.PortfolioPerformanceSeriesResponse;
import com.company.finance_api.portfolio.InstrumentListingCurrency;
import com.company.finance_api.portfolio.PortfolioPerformanceMath;
import com.company.finance_api.portfolio.external.repository.ExternalPortfolioRepository;
import com.company.finance_api.repository.PortfolioDailyPerformanceRepository;
import com.company.finance_api.repository.TransactionRepository;
import com.company.finance_api.pricing.application.CurrencyConversionService;
import com.company.finance_api.pricing.application.PriceService;
import com.company.finance_api.shared.security.CurrentUserResolver;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds and serves precomputed daily portfolio performance series. */
@Service
@RequiredArgsConstructor
@Transactional
public class PortfolioPerformanceSeriesServiceImpl implements PortfolioPerformanceSeriesService {

  private static final List<String> SUPPORTED_CURRENCIES =
      List.of("USD", "EUR", "TRY", "GBP", "JPY", "AED");

  private final PortfolioDailyPerformanceRepository dailyPerformanceRepository;
  private final ExternalPortfolioRepository externalPortfolioRepository;
  private final TransactionRepository transactionRepository;
  private final PriceService priceService;
  private final CurrencyConversionService currencyConversionService;
  private final CurrentUserResolver currentUserResolver;

  @Override
  @Transactional(readOnly = true)
  public PortfolioPerformanceSeriesResponse getMyPerformanceSeries(
      String targetCurrency, Long portfolioId, String range) {
    UUID userId = currentUserResolver.getCurrentUserId();
    String currency = currencyConversionService.normalizeCurrency(targetCurrency);
    if (portfolioId != null) {
      if (externalPortfolioRepository.findByIdAndUserId(portfolioId, userId).isEmpty()) {
        return new PortfolioPerformanceSeriesResponse(currency, null, List.of());
      }
      ensureFresh(userId, portfolioId, currency);
      List<PortfolioDailyPerformance> rows =
          dailyPerformanceRepository.findByUserIdAndExternalPortfolioIdAndCurrencyOrderByDayUtcAsc(
              userId, portfolioId, currency);
      return toSeriesResponse(currency, rows);
    }

    List<Long> portfolioIds =
        externalPortfolioRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
            .map(p -> p.getId())
            .toList();
    if (portfolioIds.isEmpty()) {
      return new PortfolioPerformanceSeriesResponse(currency, null, List.of());
    }
    for (Long id : portfolioIds) {
      ensureFresh(userId, id, currency);
    }
    List<PortfolioDailyPerformance> rows =
        dailyPerformanceRepository.findByUserIdAndCurrencyOrderByDayUtcAsc(userId, currency);
    return aggregateSeriesResponse(currency, rows);
  }

  @Override
  public void recomputePortfolioHistory(UUID userId, Long portfolioId) {
    if (portfolioId == null) {
      return;
    }
    if (externalPortfolioRepository.findByIdAndUserId(portfolioId, userId).isEmpty()) {
      return;
    }
    for (String currency : SUPPORTED_CURRENCIES) {
      recomputePortfolioHistory(userId, portfolioId, currency);
    }
  }

  @Override
  public void recomputeAllPortfolioHistory() {
    for (Object[] row : externalPortfolioRepository.findAllPortfolioIdAndUserId()) {
      Long portfolioId = row[0] instanceof Number n ? n.longValue() : null;
      UUID userId = row[1] instanceof UUID id ? id : null;
      if (portfolioId != null && userId != null) {
        recomputePortfolioHistory(userId, portfolioId);
      }
    }
  }

  private void ensureFresh(UUID userId, Long portfolioId, String currency) {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    boolean hasToday =
        dailyPerformanceRepository.existsForDay(userId, portfolioId, currency, today);
    if (!hasToday) {
      recomputePortfolioHistory(userId, portfolioId, currency);
    }
  }

  private void recomputePortfolioHistory(UUID userId, Long portfolioId, String currency) {
    List<Transaction> txs =
        transactionRepository.findByUserIdAndExternalPortfolioIdOrderByCreatedAtAsc(
            userId, portfolioId);
    dailyPerformanceRepository.deleteByUserIdAndExternalPortfolioIdAndCurrency(
        userId, portfolioId, currency);
    if (txs.isEmpty()) {
      return;
    }

    List<Transaction> ordered =
        txs.stream()
            .sorted(
                Comparator.comparing(PortfolioPerformanceSeriesServiceImpl::effectiveInstant)
                    .thenComparing(Transaction::getId))
            .toList();

    LocalDate firstDay = effectiveInstant(ordered.getFirst()).atZone(ZoneOffset.UTC).toLocalDate();
    LocalDate today = LocalDate.now(ZoneOffset.UTC);
    Map<Instrument, BigDecimal> quantities = new LinkedHashMap<>();
    List<PortfolioDailyPerformance> rows = new ArrayList<>();
    BigDecimal previousMarketValue = null;
    BigDecimal twrIndex = BigDecimal.ONE.setScale(10, RoundingMode.HALF_UP);
    int txIndex = 0;

    for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
      Instant dayEndExclusive = day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
      BigDecimal netFlow = BigDecimal.ZERO;
      boolean complete = true;

      while (txIndex < ordered.size()
          && effectiveInstant(ordered.get(txIndex)).isBefore(dayEndExclusive)) {
        Transaction tx = ordered.get(txIndex);
        BigDecimal flow = convertTradeFlow(tx, dayEndExclusive, currency);
        if (flow == null) {
          complete = false;
          flow = BigDecimal.ZERO;
        }
        netFlow =
            tx.getType() == TransactionType.BUY ? netFlow.add(flow) : netFlow.subtract(flow);
        quantities.compute(
            tx.getInstrument(),
            (_instrument, current) -> {
              BigDecimal next =
                  (current != null ? current : BigDecimal.ZERO)
                      .add(
                          tx.getType() == TransactionType.BUY
                              ? tx.getQuantity()
                              : tx.getQuantity().negate());
              return next.compareTo(BigDecimal.ZERO) > 0 ? next : null;
            });
        txIndex++;
      }

      BigDecimal marketValue = BigDecimal.ZERO;
      for (Map.Entry<Instrument, BigDecimal> entry : quantities.entrySet()) {
        BigDecimal holdingValue =
            convertHoldingValue(entry.getKey(), entry.getValue(), dayEndExclusive, currency);
        if (holdingValue == null) {
          complete = false;
          continue;
        }
        marketValue = marketValue.add(holdingValue);
      }

      BigDecimal dailyPnl =
          PortfolioPerformanceMath.dailyPnl(previousMarketValue, marketValue, netFlow);
      BigDecimal dailyReturnPct =
          PortfolioPerformanceMath.dailyReturnPct(previousMarketValue, marketValue, netFlow);
      twrIndex = PortfolioPerformanceMath.advanceTwrIndex(twrIndex, dailyReturnPct);

      PortfolioDailyPerformance row = new PortfolioDailyPerformance();
      row.setUserId(userId);
      row.setExternalPortfolioId(portfolioId);
      row.setCurrency(currency);
      row.setDayUtc(day);
      row.setMarketValue(scaleMoney(marketValue));
      row.setNetFlow(scaleMoney(netFlow));
      row.setDailyPnl(scaleMoney(dailyPnl));
      row.setDailyReturnPct(scaleNullablePct(dailyReturnPct));
      row.setTwrIndex(twrIndex);
      row.setComplete(complete);
      row.setComputedAt(Instant.now());
      rows.add(row);
      previousMarketValue = marketValue;
    }

    dailyPerformanceRepository.saveAll(rows);
  }

  private PortfolioPerformanceSeriesResponse toSeriesResponse(
      String currency, List<PortfolioDailyPerformance> rows) {
    if (rows.isEmpty()) {
      return new PortfolioPerformanceSeriesResponse(currency, null, List.of());
    }
    List<PortfolioPerformancePointResponse> points =
        rows.stream()
            .map(
                row ->
                    new PortfolioPerformancePointResponse(
                        row.getDayUtc(),
                        row.getMarketValue(),
                        row.getNetFlow(),
                        row.getDailyReturnPct(),
                        PortfolioPerformanceMath.twrPctFromIndex(row.getTwrIndex())))
            .toList();
    return new PortfolioPerformanceSeriesResponse(currency, rows.getFirst().getDayUtc(), points);
  }

  private PortfolioPerformanceSeriesResponse aggregateSeriesResponse(
      String currency, List<PortfolioDailyPerformance> rows) {
    if (rows.isEmpty()) {
      return new PortfolioPerformanceSeriesResponse(currency, null, List.of());
    }
    Map<LocalDate, AggregateRow> byDay = new TreeMap<>();
    for (PortfolioDailyPerformance row : rows) {
      AggregateRow agg = byDay.computeIfAbsent(row.getDayUtc(), ignored -> new AggregateRow());
      agg.marketValue = agg.marketValue.add(row.getMarketValue());
      agg.netFlow = agg.netFlow.add(row.getNetFlow());
      agg.complete &= row.isComplete();
    }

    BigDecimal previousMarketValue = null;
    BigDecimal twrIndex = BigDecimal.ONE.setScale(10, RoundingMode.HALF_UP);
    List<PortfolioPerformancePointResponse> points = new ArrayList<>(byDay.size());
    for (Map.Entry<LocalDate, AggregateRow> entry : byDay.entrySet()) {
      BigDecimal marketValue = scaleMoney(entry.getValue().marketValue);
      BigDecimal netFlow = scaleMoney(entry.getValue().netFlow);
      BigDecimal dailyReturnPct =
          PortfolioPerformanceMath.dailyReturnPct(previousMarketValue, marketValue, netFlow);
      twrIndex = PortfolioPerformanceMath.advanceTwrIndex(twrIndex, dailyReturnPct);
      points.add(
          new PortfolioPerformancePointResponse(
              entry.getKey(),
              marketValue,
              netFlow,
              scaleNullablePct(dailyReturnPct),
              PortfolioPerformanceMath.twrPctFromIndex(twrIndex)));
      previousMarketValue = marketValue;
    }
    return new PortfolioPerformanceSeriesResponse(currency, points.getFirst().day(), points);
  }

  private BigDecimal convertTradeFlow(
      Transaction tx, Instant asOfEndExclusive, String targetCurrency) {
    return currencyConversionService.convertAt(
        asOfEndExclusive,
        tx.getTotalAmount(),
        InstrumentListingCurrency.resolve(tx.getInstrument()),
        targetCurrency);
  }

  private BigDecimal convertHoldingValue(
      Instrument instrument, BigDecimal quantity, Instant asOfEndExclusive, String targetCurrency) {
    return priceService
        .getLatestValuationPriceBefore(instrument, asOfEndExclusive)
        .map(price -> price.getPrice().multiply(quantity))
        .map(
            value ->
                currencyConversionService.convertAt(
                    asOfEndExclusive,
                    value,
                    InstrumentListingCurrency.resolve(instrument),
                    targetCurrency))
        .map(PortfolioPerformanceSeriesServiceImpl::scaleMoney)
        .orElse(null);
  }

  private static Instant effectiveInstant(Transaction tx) {
    return tx.getAcquiredAt() != null ? tx.getAcquiredAt() : tx.getCreatedAt();
  }

  private static BigDecimal scaleMoney(BigDecimal value) {
    return (value != null ? value : BigDecimal.ZERO).setScale(6, RoundingMode.HALF_UP);
  }

  private static BigDecimal scaleNullablePct(BigDecimal value) {
    return value != null ? value.setScale(6, RoundingMode.HALF_UP) : null;
  }

  private static final class AggregateRow {
    private BigDecimal marketValue = BigDecimal.ZERO;
    private BigDecimal netFlow = BigDecimal.ZERO;
    @SuppressWarnings("unused")
    private boolean complete = true;
  }
}
