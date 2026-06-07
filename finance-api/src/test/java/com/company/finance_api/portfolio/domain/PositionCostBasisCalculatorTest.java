package com.company.finance_api.portfolio.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.portfolio.domain.Transaction;
import com.company.finance_api.profile.domain.User;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class PositionCostBasisCalculatorTest {

  private final PositionCostBasisCalculator calculator = new PositionCostBasisCalculator();

  @Test
  void should_calculate_wac_for_buy_then_sell() {
    Transaction buy = tx(1L, Instant.parse("2026-01-01T10:00:00Z"), "100", "10", true);
    Transaction sell = tx(2L, Instant.parse("2026-01-01T11:00:00Z"), "120", "5", false);

    PositionCostBasisCalculator.PositionCostBasis result = calculator.calculate(List.of(sell, buy));

    assertEquals(new BigDecimal("5.000000"), result.quantity().setScale(6));
    assertEquals(new BigDecimal("500.000000"), result.totalCost().setScale(6));
    assertEquals(new BigDecimal("100.000000"), result.averageCost().setScale(6));
  }

  @Test
  void should_calculate_wac_for_multiple_buys_then_sell() {
    Transaction buy1 = tx(1L, Instant.parse("2026-01-01T10:00:00Z"), "100", "10", true);
    Transaction buy2 = tx(2L, Instant.parse("2026-01-01T11:00:00Z"), "200", "10", true);
    Transaction sell = tx(3L, Instant.parse("2026-01-01T12:00:00Z"), "300", "5", false);

    PositionCostBasisCalculator.PositionCostBasis result =
        calculator.calculate(List.of(sell, buy2, buy1));

    assertEquals(new BigDecimal("15.000000"), result.quantity().setScale(6));
    assertEquals(new BigDecimal("2250.000000"), result.totalCost().setScale(6));
    assertEquals(new BigDecimal("150.000000"), result.averageCost().setScale(6));
  }

  @Test
  void should_clamp_total_cost_to_zero_when_position_fully_closed() {
    Transaction buy = tx(1L, Instant.parse("2026-01-01T10:00:00Z"), "100", "2", true);
    Transaction sell = tx(2L, Instant.parse("2026-01-01T11:00:00Z"), "50", "2", false);

    PositionCostBasisCalculator.PositionCostBasis result = calculator.calculate(List.of(buy, sell));

    assertEquals(BigDecimal.ZERO, result.quantity());
    assertEquals(BigDecimal.ZERO, result.totalCost());
    assertEquals(BigDecimal.ZERO, result.averageCost());
  }

  @Test
  void validateLedger_shouldPass_forBalancedLedger() {
    Transaction buy = tx(1L, Instant.parse("2026-01-01T10:00:00Z"), "100", "5", true);
    Transaction sell = tx(2L, Instant.parse("2026-01-01T11:00:00Z"), "120", "3", false);

    PositionCostBasisCalculator.validateLedger(List.of(buy, sell));
  }

  @Test
  void validateLedger_shouldReject_whenSellExceedsHoldings() {
    Transaction sell = tx(1L, Instant.parse("2026-01-01T11:00:00Z"), "120", "5", false);

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> PositionCostBasisCalculator.validateLedger(List.of(sell)));
    assertEquals("Cannot delete: would leave insufficient holdings for later sells", ex.getMessage());
  }

  @Test
  void calculateHoldingsBefore_shouldExcludeSameDayBuys_forPastSellDate() {
    Transaction sepBuy =
        pastBuy(1L, LocalDate.of(2025, 9, 10), "100", "10");
    Transaction octBuy =
        pastBuy(2L, LocalDate.of(2025, 10, 10), "110", "10");
    Transaction novBuy =
        pastBuy(3L, LocalDate.of(2025, 11, 10), "120", "10");

    Instant oct8Cutoff = LocalDate.of(2025, 10, 8).atStartOfDay(ZoneOffset.UTC).toInstant();
    PositionCostBasisCalculator.PositionCostBasis onOct8 =
        calculator.calculateHoldingsBefore(List.of(sepBuy, octBuy, novBuy), oct8Cutoff);
    assertEquals(new BigDecimal("10.000000"), onOct8.quantity().setScale(6));

    Instant oct10Cutoff = LocalDate.of(2025, 10, 10).atStartOfDay(ZoneOffset.UTC).toInstant();
    PositionCostBasisCalculator.PositionCostBasis onOct10 =
        calculator.calculateHoldingsBefore(List.of(sepBuy, octBuy, novBuy), oct10Cutoff);
    assertEquals(new BigDecimal("10.000000"), onOct10.quantity().setScale(6));

    Instant oct11Cutoff = LocalDate.of(2025, 10, 11).atStartOfDay(ZoneOffset.UTC).toInstant();
    PositionCostBasisCalculator.PositionCostBasis onOct11 =
        calculator.calculateHoldingsBefore(List.of(sepBuy, octBuy, novBuy), oct11Cutoff);
    assertEquals(new BigDecimal("20.000000"), onOct11.quantity().setScale(6));
  }

  private static Transaction pastBuy(
      Long id, LocalDate acquiredDay, String price, String quantity) {
    User user = mock(User.class);
    Instrument instrument = mock(Instrument.class);
    Instant acquiredAt = acquiredDay.atStartOfDay(ZoneOffset.UTC).toInstant();
    Transaction tx =
        Transaction.buy(
            user,
            instrument,
            null,
            new BigDecimal(price),
            new BigDecimal(quantity),
            com.company.finance_api.portfolio.domain.enums.PurchaseMode.PAST,
            acquiredAt,
            new BigDecimal(price),
            com.company.finance_api.portfolio.domain.enums.TradeInputMode.LOTS,
            "TRY",
            new BigDecimal(price).multiply(new BigDecimal(quantity)),
            BigDecimal.ONE,
            "PAST_BOUGHT");
    setField(tx, "id", id);
    setField(tx, "createdAt", acquiredAt);
    return tx;
  }

  private static Transaction tx(
      Long id, Instant createdAt, String price, String quantity, boolean buy) {
    User user = mock(User.class);
    Instrument instrument = mock(Instrument.class);
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
