package com.company.finance_api.portfolio;

import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Transaction listesinden pozisyon maliyet tabanını (quantity, totalCost, averageCost) hesaplar.
 */
@Component
public class PositionCostBasisCalculator {

  private static final int SCALE = 6;

  /**
   * Etkin zamanı {@code cutoffExclusive} öncesinde kalan transaction'larla holdings hesaplar (ör.
   * dün kapanış mark-to-market).
   */
  public PositionCostBasis calculateHoldingsBefore(
      List<Transaction> transactions, Instant cutoffExclusive) {
    List<Transaction> ordered =
        transactions.stream()
            .filter(tx -> effectiveInstant(tx).isBefore(cutoffExclusive))
            .sorted(
                Comparator.comparing(PositionCostBasisCalculator::effectiveInstant)
                    .thenComparing(Transaction::getId))
            .toList();
    return foldCostBasis(ordered);
  }

  /** Tüm transaction'ları uygulayarak güncel pozisyon maliyet tabanını hesaplar. */
  public PositionCostBasis calculate(List<Transaction> transactions) {
    List<Transaction> ordered =
        transactions.stream()
            .sorted(
                Comparator.comparing(PositionCostBasisCalculator::effectiveInstant)
                    .thenComparing(Transaction::getId))
            .toList();
    return foldCostBasis(ordered);
  }

  private static Instant effectiveInstant(Transaction tx) {
    Instant acquired = tx.getAcquiredAt();
    return acquired != null ? acquired : tx.getCreatedAt();
  }

  private PositionCostBasis foldCostBasis(List<Transaction> ordered) {
    BigDecimal quantity = BigDecimal.ZERO;
    BigDecimal totalCost = BigDecimal.ZERO;

    for (Transaction tx : ordered) {
      BigDecimal txQuantity = tx.getQuantity();
      BigDecimal txTotalAmount = tx.getPrice().multiply(txQuantity);

      if (tx.getType() == TransactionType.BUY) {
        quantity = quantity.add(txQuantity);
        totalCost = totalCost.add(txTotalAmount);
        continue;
      }

      if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
        quantity = BigDecimal.ZERO;
        totalCost = BigDecimal.ZERO;
        continue;
      }

      BigDecimal avgCostBeforeSell = totalCost.divide(quantity, SCALE, RoundingMode.HALF_UP);
      quantity = quantity.subtract(txQuantity);
      totalCost = totalCost.subtract(avgCostBeforeSell.multiply(txQuantity));
    }

    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
      return new PositionCostBasis(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    if (totalCost.compareTo(BigDecimal.ZERO) < 0) {
      totalCost = BigDecimal.ZERO;
    }

    BigDecimal averageCost = totalCost.divide(quantity, SCALE, RoundingMode.HALF_UP);
    return new PositionCostBasis(quantity, totalCost, averageCost);
  }

  /** Hesaplanmış pozisyon miktarı, toplam maliyet ve ortalama maliyet snapshot'ı. */
  public record PositionCostBasis(
      BigDecimal quantity, BigDecimal totalCost, BigDecimal averageCost) {}
}
