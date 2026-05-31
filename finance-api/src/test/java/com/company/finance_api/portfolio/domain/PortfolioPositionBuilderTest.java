package com.company.finance_api.portfolio.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.portfolio.domain.Transaction;
import com.company.finance_api.profile.domain.User;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.pricing.domain.enums.PriceType;
import com.company.finance_api.pricing.application.PriceService;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PortfolioPositionBuilderTest {

  @Test
  void should_build_position_with_price() {
    PositionCostBasisCalculator costCalculator = mock(PositionCostBasisCalculator.class);
    PriceService priceService = mock(PriceService.class);
    PortfolioPositionBuilder builder = new PortfolioPositionBuilder(costCalculator, priceService);
    Instrument instrument = mock(Instrument.class);
    List<Transaction> txs = List.of(mock(Transaction.class));

    when(costCalculator.calculate(txs))
        .thenReturn(
            new PositionCostBasisCalculator.PositionCostBasis(
                new BigDecimal("5.000000"),
                new BigDecimal("500.000000"),
                new BigDecimal("100.000000")));
    when(priceService.getLatestValuationPrice(instrument))
        .thenReturn(
            Optional.of(
                new InstrumentPrice(
                    instrument, PriceType.MARKET, new BigDecimal("120.000000"), Instant.now())));

    Optional<PortfolioPosition> result = builder.build(instrument, txs);

    assertTrue(result.isPresent());
    assertEquals(new BigDecimal("5.000000"), result.get().quantity().setScale(6));
    assertEquals(new BigDecimal("500.000000"), result.get().totalCost().setScale(6));
    assertEquals(new BigDecimal("120.000000"), result.get().currentPrice().setScale(6));
    assertEquals(new BigDecimal("600.000000"), result.get().currentValue().setScale(6));
    assertEquals(new BigDecimal("100.000000"), result.get().unrealizedPnl().setScale(6));
    assertTrue(result.get().hasPrice());
  }

  @Test
  void should_build_position_without_price_as_zero_values() {
    PositionCostBasisCalculator costCalculator = mock(PositionCostBasisCalculator.class);
    PriceService priceService = mock(PriceService.class);
    PortfolioPositionBuilder builder = new PortfolioPositionBuilder(costCalculator, priceService);
    Instrument instrument = mock(Instrument.class);
    List<Transaction> txs = List.of(mock(Transaction.class));

    when(costCalculator.calculate(txs))
        .thenReturn(
            new PositionCostBasisCalculator.PositionCostBasis(
                new BigDecimal("3.000000"),
                new BigDecimal("450.000000"),
                new BigDecimal("150.000000")));
    when(priceService.getLatestValuationPrice(instrument)).thenReturn(Optional.empty());

    Optional<PortfolioPosition> result = builder.build(instrument, txs);

    assertTrue(result.isPresent());
    assertEquals(BigDecimal.ZERO, result.get().currentPrice());
    assertEquals(BigDecimal.ZERO, result.get().currentValue());
    assertEquals(BigDecimal.ZERO, result.get().unrealizedPnl());
    assertFalse(result.get().hasPrice());
  }

  @Test
  void should_return_empty_for_non_positive_quantity() {
    PositionCostBasisCalculator costCalculator = mock(PositionCostBasisCalculator.class);
    PriceService priceService = mock(PriceService.class);
    PortfolioPositionBuilder builder = new PortfolioPositionBuilder(costCalculator, priceService);
    Instrument instrument = mock(Instrument.class);
    List<Transaction> txs = List.of(mock(Transaction.class));

    when(costCalculator.calculate(txs))
        .thenReturn(
            new PositionCostBasisCalculator.PositionCostBasis(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

    Optional<PortfolioPosition> result = builder.build(instrument, txs);

    assertTrue(result.isEmpty());
  }

  @Test
  void should_build_position_with_expected_wac_after_buy_buy_sell() {
    PositionCostBasisCalculator costCalculator = new PositionCostBasisCalculator();
    PriceService priceService = mock(PriceService.class);
    PortfolioPositionBuilder builder = new PortfolioPositionBuilder(costCalculator, priceService);

    Instrument instrument =
        new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE);
    User user = new User("u@example.com", "user");
    Transaction buy1 =
        tx(1L, Instant.parse("2026-01-01T10:00:00Z"), user, instrument, "100", "10", true);
    Transaction buy2 =
        tx(2L, Instant.parse("2026-01-01T11:00:00Z"), user, instrument, "200", "10", true);
    Transaction sell =
        tx(3L, Instant.parse("2026-01-01T12:00:00Z"), user, instrument, "300", "5", false);
    List<Transaction> txs = List.of(sell, buy2, buy1);

    when(priceService.getLatestValuationPrice(instrument)).thenReturn(Optional.empty());

    Optional<PortfolioPosition> result = builder.build(instrument, txs);

    assertTrue(result.isPresent());
    assertEquals(new BigDecimal("15.000000"), result.get().quantity().setScale(6));
    assertEquals(new BigDecimal("2250.000000"), result.get().totalCost().setScale(6));
    assertEquals(new BigDecimal("150.000000"), result.get().averageCost().setScale(6));
  }

  private static Transaction tx(
      Long id,
      Instant createdAt,
      User user,
      Instrument instrument,
      String price,
      String quantity,
      boolean buy) {
    Transaction tx =
        buy
            ? Transaction.buy(user, instrument, new BigDecimal(price), new BigDecimal(quantity))
            : Transaction.sell(user, instrument, new BigDecimal(price), new BigDecimal(quantity));
    setField(tx, "id", id);
    setField(tx, "createdAt", createdAt);
    return tx;
  }

  private static void setField(Object target, String fieldName, Object value) {
    try {
      Field field = target.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
