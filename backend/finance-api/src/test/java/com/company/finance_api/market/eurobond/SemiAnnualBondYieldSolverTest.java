package com.company.finance_api.market.eurobond;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SemiAnnualBondYieldSolverTest {

    @Test
    void nearParCouponBond_hasYieldNearCoupon() {
        LocalDate asOf = LocalDate.of(2025, 1, 2);
        LocalDate maturity = LocalDate.of(2035, 1, 2);
        BigDecimal y = SemiAnnualBondYieldSolver.annualPercentFromCleanPrice(
                BigDecimal.valueOf(100.0), BigDecimal.valueOf(5.0), asOf, maturity);
        assertThat(y).isNotNull();
        assertThat(y.doubleValue()).isBetween(4.5, 5.5);
    }

    @Test
    void inverseYield_roundTripsToOriginalPrice() {
        LocalDate asOf = LocalDate.of(2025, 1, 2);
        LocalDate maturity = LocalDate.of(2035, 1, 2);
        BigDecimal price = BigDecimal.valueOf(98.5);
        BigDecimal coupon = BigDecimal.valueOf(6.0);
        BigDecimal y = SemiAnnualBondYieldSolver.annualPercentFromCleanPrice(price, coupon, asOf, maturity);
        assertThat(y).isNotNull();
        BigDecimal p2 = SemiAnnualBondYieldSolver.cleanPriceFromAnnualYieldPercent(y, coupon, asOf, maturity);
        assertThat(p2).isNotNull();
        assertThat(p2.subtract(price).abs().doubleValue()).isLessThan(0.02);
    }

    @Test
    void premiumPrice_lowersYieldVsCoupon() {
        LocalDate asOf = LocalDate.of(2025, 1, 2);
        LocalDate maturity = LocalDate.of(2035, 1, 2);
        BigDecimal yDisc = SemiAnnualBondYieldSolver.annualPercentFromCleanPrice(
                BigDecimal.valueOf(95.0), BigDecimal.valueOf(5.0), asOf, maturity);
        BigDecimal yPrem = SemiAnnualBondYieldSolver.annualPercentFromCleanPrice(
                BigDecimal.valueOf(105.0), BigDecimal.valueOf(5.0), asOf, maturity);
        assertThat(yDisc).isNotNull();
        assertThat(yPrem).isNotNull();
        assertThat(yDisc.doubleValue()).isGreaterThan(yPrem.doubleValue());
    }
}
