package com.company.marketdataservice.rates;

import com.company.marketdataservice.provider.tcmb.BondEodPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PolicyRateLatestResolverTest {

    @Test
    void detectsIncreaseFromPriorStep() {
        var daily = List.of(
                new BondEodPoint(LocalDate.of(2024, 1, 1), bd("40")),
                new BondEodPoint(LocalDate.of(2024, 2, 1), bd("45")),
                new BondEodPoint(LocalDate.of(2024, 3, 1), bd("50")),
                new BondEodPoint(LocalDate.of(2024, 3, 15), bd("50"))
        );
        PolicyRateLatestResolver.Result r = PolicyRateLatestResolver.resolve(daily);
        assertEquals(bd("50.00"), r.value());
        assertEquals(LocalDate.of(2024, 3, 15), r.decisionDate());
        assertEquals(PolicyRateLatestResolver.CHANGE_UP, r.changeVsPrior());
    }

    @Test
    void detectsDecrease() {
        var daily = List.of(
                new BondEodPoint(LocalDate.of(2024, 1, 1), bd("50")),
                new BondEodPoint(LocalDate.of(2024, 2, 1), bd("45"))
        );
        PolicyRateLatestResolver.Result r = PolicyRateLatestResolver.resolve(daily);
        assertEquals(PolicyRateLatestResolver.CHANGE_DOWN, r.changeVsPrior());
    }

    @Test
    void unchangedWhenSingleLevel() {
        var daily = List.of(
                new BondEodPoint(LocalDate.of(2024, 1, 1), bd("50")),
                new BondEodPoint(LocalDate.of(2024, 2, 1), bd("50"))
        );
        PolicyRateLatestResolver.Result r = PolicyRateLatestResolver.resolve(daily);
        assertEquals(PolicyRateLatestResolver.CHANGE_UNCHANGED, r.changeVsPrior());
    }

    @Test
    void emptyReturnsNull() {
        assertNull(PolicyRateLatestResolver.resolve(List.of()));
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
