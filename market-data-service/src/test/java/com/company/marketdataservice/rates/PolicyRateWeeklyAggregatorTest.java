package com.company.marketdataservice.rates;

import com.company.marketdataservice.provider.tcmb.BondEodPoint;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolicyRateWeeklyAggregatorTest {

    @Test
    void lastObservationInWeekWins() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        List<BondEodPoint> daily = List.of(
                new BondEodPoint(LocalDate.of(2024, 1, 2), bd("10")),
                new BondEodPoint(LocalDate.of(2024, 1, 4), bd("12")),
                new BondEodPoint(LocalDate.of(2024, 1, 9), bd("15"))
        );
        Map<LocalDate, PolicyRateWeeklyAggregator.WeekPolicyPick> w =
                PolicyRateWeeklyAggregator.lastPickByWeekMonday(daily, start, end);
        assertEquals(5, w.size());
        assertEquals(bd("12"), w.get(LocalDate.of(2024, 1, 1)).ratePercent());
        assertEquals(LocalDate.of(2024, 1, 4), w.get(LocalDate.of(2024, 1, 1)).evdsObservationDate());
        assertEquals(bd("15"), w.get(LocalDate.of(2024, 1, 8)).ratePercent());
        assertEquals(LocalDate.of(2024, 1, 9), w.get(LocalDate.of(2024, 1, 8)).evdsObservationDate());
        assertEquals(bd("15"), w.get(LocalDate.of(2024, 1, 15)).ratePercent());
        assertEquals(bd("15"), w.get(LocalDate.of(2024, 1, 22)).ratePercent());
        assertEquals(bd("15"), w.get(LocalDate.of(2024, 1, 29)).ratePercent());
    }

    @Test
    void sparseMonthlySeriesCarriesForwardUntilNextObservation() {
        LocalDate start = LocalDate.of(2024, 2, 5);
        LocalDate end = LocalDate.of(2024, 2, 29);
        List<BondEodPoint> pts = List.of(
                new BondEodPoint(LocalDate.of(2024, 1, 31), bd("45")),
                new BondEodPoint(LocalDate.of(2024, 3, 1), bd("50"))
        );
        Map<LocalDate, PolicyRateWeeklyAggregator.WeekPolicyPick> w =
                PolicyRateWeeklyAggregator.lastPickByWeekMonday(pts, start, end);
        assertEquals(4, w.size());
        assertEquals(bd("45"), w.get(LocalDate.of(2024, 2, 5)).ratePercent());
        assertEquals(LocalDate.of(2024, 1, 31), w.get(LocalDate.of(2024, 2, 5)).evdsObservationDate());
        assertEquals(bd("45"), w.get(LocalDate.of(2024, 2, 12)).ratePercent());
        assertEquals(bd("45"), w.get(LocalDate.of(2024, 2, 19)).ratePercent());
        assertEquals(bd("45"), w.get(LocalDate.of(2024, 2, 26)).ratePercent());
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
