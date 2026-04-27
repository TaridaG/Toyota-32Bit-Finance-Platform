package com.company.finance_api.portfolio;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

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

        PositionCostBasisCalculator.PositionCostBasis result = calculator.calculate(List.of(sell, buy2, buy1));

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

    private static Transaction tx(Long id, Instant createdAt, String price, String quantity, boolean buy) {
        User user = mock(User.class);
        Instrument instrument = mock(Instrument.class);
        Transaction tx = buy
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
