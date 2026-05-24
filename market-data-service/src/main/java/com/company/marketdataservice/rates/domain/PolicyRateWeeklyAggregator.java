package com.company.marketdataservice.rates.domain;
import com.company.marketdataservice.shared.provider.tcmb.BondEodPoint;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Günlük politika faizi noktalarını haftalık bucket'lara aggregate eder.
 */
public final class PolicyRateWeeklyAggregator {

    /**
     * @param evdsObservationDate date of the EVDS row that supplied {@link #ratePercent()} for that ISO week
     */
    public record WeekPolicyPick(BigDecimal ratePercent, LocalDate evdsObservationDate) {
    }

    private PolicyRateWeeklyAggregator() {
    }

    public static Map<LocalDate, WeekPolicyPick> lastPickByWeekMonday(
            List<BondEodPoint> dailyAnyOrder,
            LocalDate rangeStart,
            LocalDate rangeEnd
    ) {
        List<BondEodPoint> sorted = dailyAnyOrder.stream()
                .filter(p -> !p.date().isAfter(rangeEnd))
                .sorted(Comparator.comparing(BondEodPoint::date))
                .toList();
        LocalDate firstMonday = rangeStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastMonday = rangeEnd.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<LocalDate, WeekPolicyPick> out = new LinkedHashMap<>();
        for (LocalDate m = firstMonday; !m.isAfter(lastMonday); m = m.plusWeeks(1)) {
            LocalDate weekEnd = m.plusDays(6);
            BondEodPoint chosen = null;
            for (BondEodPoint p : sorted) {
                if (!p.date().isAfter(weekEnd)) {
                    chosen = p;
                }
            }
            if (chosen != null) {
                out.put(m, new WeekPolicyPick(chosen.value(), chosen.date()));
            }
        }
        return out;
    }
}
