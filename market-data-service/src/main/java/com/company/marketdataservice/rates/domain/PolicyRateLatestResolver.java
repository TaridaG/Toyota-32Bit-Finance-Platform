package com.company.marketdataservice.rates.domain;
import com.company.marketdataservice.shared.provider.tcmb.BondEodPoint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;


/**
 * Politika faizi haftalık serisinden en güncel noktayı seçer.
 */
public final class PolicyRateLatestResolver {

    public static final String CHANGE_UNCHANGED = "UNCHANGED";
    public static final String CHANGE_UP = "UP";
    public static final String CHANGE_DOWN = "DOWN";

    /**
     * Politika faizi latest snapshot sonucu.
         * @param value güncel faiz oranı (yüzde)
         * @param decisionDate son EVDS gözlem tarihi
         * @param changeVsPrior bir önceki farklı değere göre değişim yönü ({@link #CHANGE_UP}, {@link #CHANGE_DOWN}, {@link #CHANGE_UNCHANGED})
         */
    public record Result(BigDecimal value, LocalDate decisionDate, String changeVsPrior) {}

    private PolicyRateLatestResolver() {
    }

    /**
     * @param dailyAscending points sorted by {@link BondEodPoint#date()} ascending
     */
    public static Result resolve(List<BondEodPoint> dailyAscending) {
        if (dailyAscending == null || dailyAscending.isEmpty()) {
            return null;
        }
        BondEodPoint last = dailyAscending.get(dailyAscending.size() - 1);
        BigDecimal lastVal = last.value();
        LocalDate lastDate = last.date();
        BigDecimal priorVal = null;
        for (int i = dailyAscending.size() - 2; i >= 0; i--) {
            BondEodPoint p = dailyAscending.get(i);
            if (p.value().compareTo(lastVal) != 0) {
                priorVal = p.value();
                break;
            }
        }
        String change;
        if (priorVal == null) {
            change = CHANGE_UNCHANGED;
        } else {
            int c = lastVal.compareTo(priorVal);
            if (c > 0) {
                change = CHANGE_UP;
            } else if (c < 0) {
                change = CHANGE_DOWN;
            } else {
                change = CHANGE_UNCHANGED;
            }
        }
        return new Result(lastVal.setScale(2, RoundingMode.HALF_UP), lastDate, change);
    }
}
