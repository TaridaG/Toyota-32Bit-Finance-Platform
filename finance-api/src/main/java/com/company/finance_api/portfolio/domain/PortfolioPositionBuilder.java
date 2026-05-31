package com.company.finance_api.portfolio.domain;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.portfolio.domain.Transaction;
import com.company.finance_api.pricing.application.PriceService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Transaction geçmişi ve güncel fiyat ile {@link PortfolioPosition} oluşturur. */
@Component
@RequiredArgsConstructor
public class PortfolioPositionBuilder {

  private final PositionCostBasisCalculator positionCostBasisCalculator;
  private final PriceService priceService;

  /** Enstrüman ve transaction listesinden pozitif miktarlı bir portfolio pozisyonu üretir. */
  public Optional<PortfolioPosition> build(Instrument instrument, List<Transaction> transactions) {
    PositionCostBasisCalculator.PositionCostBasis costBasis =
        positionCostBasisCalculator.calculate(transactions);
    BigDecimal quantity = costBasis.quantity();
    if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
      return Optional.empty();
    }

    BigDecimal currentPrice = BigDecimal.ZERO;
    BigDecimal currentValue = BigDecimal.ZERO;
    BigDecimal unrealizedPnl = BigDecimal.ZERO;
    boolean hasPrice = false;

    var priceOpt = priceService.getLatestValuationPrice(instrument);
    if (priceOpt.isPresent()) {
      hasPrice = true;
      currentPrice = priceOpt.get().getPrice();
      currentValue = currentPrice.multiply(quantity);
      unrealizedPnl = currentValue.subtract(costBasis.totalCost());
    }

    return Optional.of(
        new PortfolioPosition(
            instrument,
            quantity,
            costBasis.totalCost(),
            costBasis.averageCost(),
            currentPrice,
            currentValue,
            unrealizedPnl,
            hasPrice));
  }
}
