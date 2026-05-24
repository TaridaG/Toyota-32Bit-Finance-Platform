package com.company.marketdataservice.bond.infrastructure.application;
import com.company.marketdataservice.shared.provider.tcmb.BondEodPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BondHistoryForwardFillTest {

    @Test
    void expandCarriesLastPrintAcrossGaps() {
        LocalDate a = LocalDate.of(2025, 1, 6);
        LocalDate b = LocalDate.of(2025, 1, 10);
        List<BondEodPoint> sparse =
                List.of(new BondEodPoint(LocalDate.of(2025, 1, 6), new BigDecimal("40.00")), new BondEodPoint(LocalDate.of(2025, 1, 10), new BigDecimal("41.00")));
        List<BondEodPoint> dense = BondHistoryForwardFill.expand(sparse, a, b, null);
        assertEquals(5, dense.size());
        assertEquals(new BigDecimal("40.00"), dense.get(0).value());
        assertEquals(new BigDecimal("40.00"), dense.get(1).value());
        assertEquals(new BigDecimal("40.00"), dense.get(2).value());
        assertEquals(new BigDecimal("40.00"), dense.get(3).value());
        assertEquals(new BigDecimal("41.00"), dense.get(4).value());
    }

    @Test
    void expandUsesSeedBeforeRange() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 3);
        List<BondEodPoint> sparse = List.of();
        List<BondEodPoint> dense = BondHistoryForwardFill.expand(sparse, start, end, new BigDecimal("39.50"));
        assertEquals(3, dense.size());
        assertEquals(new BigDecimal("39.50"), dense.get(2).value());
    }
}
