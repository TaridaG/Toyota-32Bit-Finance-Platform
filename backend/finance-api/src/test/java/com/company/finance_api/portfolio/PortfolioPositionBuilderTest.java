package com.company.finance_api.portfolio;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.Transaction;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.service.PriceService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortfolioPositionBuilderTest {

    @Test
    void should_build_position_with_price() {
        PositionCostBasisCalculator costCalculator = mock(PositionCostBasisCalculator.class);
        PriceService priceService = mock(PriceService.class);
        PortfolioPositionBuilder builder = new PortfolioPositionBuilder(costCalculator, priceService);
        Instrument instrument = mock(Instrument.class);
        List<Transaction> txs = List.of(mock(Transaction.class));

        when(costCalculator.calculate(txs)).thenReturn(
                new PositionCostBasisCalculator.PositionCostBasis(
                        new BigDecimal("5.000000"),
                        new BigDecimal("500.000000"),
                        new BigDecimal("100.000000")
                )
        );
        when(priceService.getLatestValuationPrice(instrument)).thenReturn(Optional.of(
                new InstrumentPrice(instrument, PriceType.MARKET, new BigDecimal("120.000000"), Instant.now())
        ));

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

        when(costCalculator.calculate(txs)).thenReturn(
                new PositionCostBasisCalculator.PositionCostBasis(
                        new BigDecimal("3.000000"),
                        new BigDecimal("450.000000"),
                        new BigDecimal("150.000000")
                )
        );
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

        when(costCalculator.calculate(txs)).thenReturn(
                new PositionCostBasisCalculator.PositionCostBasis(
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                )
        );

        Optional<PortfolioPosition> result = builder.build(instrument, txs);

        assertTrue(result.isEmpty());
    }
}
