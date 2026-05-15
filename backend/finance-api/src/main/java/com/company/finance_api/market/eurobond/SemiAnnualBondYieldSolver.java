package com.company.finance_api.market.eurobond;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Semi-annual US-style bond math: clean price quoted on 100 nominal, coupons paid twice per year.
 * Returns a street-style annualised figure as {@code semiYield * 2 * 100} (percent points).
 */
public final class SemiAnnualBondYieldSolver {

    private SemiAnnualBondYieldSolver() {}

    public static BigDecimal annualPercentFromCleanPrice(
            BigDecimal cleanPricePct, BigDecimal annualCouponPct, LocalDate asOf, LocalDate maturityDate) {
        if (cleanPricePct == null
                || annualCouponPct == null
                || asOf == null
                || maturityDate == null
                || !asOf.isBefore(maturityDate)) {
            return null;
        }
        long days = ChronoUnit.DAYS.between(asOf, maturityDate);
        if (days <= 0) {
            return null;
        }
        int n = (int) Math.max(1, Math.min(400, Math.round((days / 365.25) * 2.0)));
        double price = cleanPricePct.doubleValue();
        double cAnnual = annualCouponPct.doubleValue();
        double semiCouponCash = cAnnual / 100.0d * 100.0d / 2.0d;

        double y = 0.045d;
        for (int i = 0; i < 60; i++) {
            double pv = priceFromSemiYield(y, semiCouponCash, n);
            double dpv = derivPriceFromSemiYield(y, semiCouponCash, n);
            double f = pv - price;
            if (Math.abs(f) < 1e-9) {
                break;
            }
            if (Math.abs(dpv) < 1e-14) {
                return null;
            }
            y -= f / dpv;
            if (y < 1e-9) {
                y = 1e-9;
            }
            if (y > 2.5d) {
                y = 2.5d;
            }
        }
        double annualPct = y * 2.0d * 100.0d;
        if (!Double.isFinite(annualPct) || annualPct <= 0 || annualPct > 200) {
            return null;
        }
        return BigDecimal.valueOf(annualPct).setScale(6, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Inverse of {@link #annualPercentFromCleanPrice}: finds clean price (100 par) matching a target annual yield %.
     */
    public static BigDecimal cleanPriceFromAnnualYieldPercent(
            BigDecimal annualYieldPct, BigDecimal annualCouponPct, LocalDate asOf, LocalDate maturityDate) {
        if (annualYieldPct == null
                || annualCouponPct == null
                || asOf == null
                || maturityDate == null
                || !asOf.isBefore(maturityDate)) {
            return null;
        }
        double target = annualYieldPct.doubleValue();
        if (!Double.isFinite(target) || target <= 0 || target > 80) {
            return null;
        }
        double lo = 1.0d;
        double hi = 250.0d;
        for (int i = 0; i < 55; i++) {
            double mid = (lo + hi) / 2.0d;
            BigDecimal yMid = annualPercentFromCleanPrice(BigDecimal.valueOf(mid), annualCouponPct, asOf, maturityDate);
            if (yMid == null) {
                return null;
            }
            double yv = yMid.doubleValue();
            if (Math.abs(yv - target) < 1e-4d) {
                return BigDecimal.valueOf(mid).setScale(6, java.math.RoundingMode.HALF_UP);
            }
            if (yv > target) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return BigDecimal.valueOf((lo + hi) / 2.0d).setScale(6, java.math.RoundingMode.HALF_UP);
    }

    private static double priceFromSemiYield(double y, double semiCouponCash, int n) {
        double sum = 0.0d;
        for (int k = 1; k <= n; k++) {
            sum += semiCouponCash * Math.pow(1.0d + y, -k);
        }
        sum += 100.0d * Math.pow(1.0d + y, -n);
        return sum;
    }

    private static double derivPriceFromSemiYield(double y, double semiCouponCash, int n) {
        double sum = 0.0d;
        for (int k = 1; k <= n; k++) {
            sum += -k * semiCouponCash * Math.pow(1.0d + y, -k - 1);
        }
        sum += -n * 100.0d * Math.pow(1.0d + y, -n - 1);
        return sum;
    }
}
