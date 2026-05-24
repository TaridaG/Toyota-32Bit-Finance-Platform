package com.company.marketdataservice.rates.domain;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRateHistoryPointDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.PolicyRatePointSourceQuality;
import com.company.marketdataservice.shared.provider.tcmb.BondEodPoint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;


/**
 * Politika faizi haftalık serisinden aylık özet üretir.
 */
public final class PolicyRateMonthlyAggregator {

    private PolicyRateMonthlyAggregator() {
    }

    /**
     * Seriyi aggregate eder.
         * @param dailyAscending girdi parametresi
         * @param rangeStartInclusive girdi parametresi
         * @param rangeEndInclusive girdi parametresi
         * @return işlem sonucu
         */
    public static List<PolicyRateHistoryPointDto> aggregate(
            List<BondEodPoint> dailyAscending,
            LocalDate rangeStartInclusive,
            LocalDate rangeEndInclusive
    ) {
        if (rangeEndInclusive.isBefore(rangeStartInclusive)) {
            return List.of();
        }
        List<BondEodPoint> sorted = new ArrayList<>(dailyAscending);
        sorted.sort(Comparator.comparing(BondEodPoint::date));

        YearMonth windowStartYm = YearMonth.from(rangeStartInclusive);
        YearMonth windowEndYm = YearMonth.from(rangeEndInclusive);

        NavigableMap<YearMonth, BigDecimal> lastRawInMonth = new TreeMap<>();
        for (BondEodPoint p : sorted) {
            if (p.date().isBefore(rangeStartInclusive) || p.date().isAfter(rangeEndInclusive)) {
                continue;
            }
            YearMonth ym = YearMonth.from(p.date());
            lastRawInMonth.put(ym, p.value());
        }

        YearMonth firstDataYm = lastRawInMonth.isEmpty() ? null : lastRawInMonth.firstKey();
        BigDecimal firstDataVal = firstDataYm == null ? null : lastRawInMonth.get(firstDataYm);

        List<PolicyRateHistoryPointDto> out = new ArrayList<>();
        BigDecimal carry = null;

        for (YearMonth ym = windowStartYm; !ym.isAfter(windowEndYm); ym = ym.plusMonths(1)) {
            LocalDate monthEnd = ym.atEndOfMonth();
            LocalDate pointDate = monthEnd.isAfter(rangeEndInclusive) ? rangeEndInclusive : monthEnd;

            BigDecimal rawMonthLast = lastRawInMonth.get(ym);
            if (rawMonthLast != null) {
                carry = rawMonthLast;
                out.add(new PolicyRateHistoryPointDto(pointDate, scale2(carry)));
                continue;
            }
            if (carry != null) {
                out.add(new PolicyRateHistoryPointDto(pointDate, scale2(carry), PolicyRatePointSourceQuality.PARTIAL));
                continue;
            }
            if (firstDataYm != null && firstDataVal != null && ym.isBefore(firstDataYm)) {
                out.add(new PolicyRateHistoryPointDto(pointDate, scale2(firstDataVal), PolicyRatePointSourceQuality.PARTIAL));
            }
        }
        return out;
    }

    private static BigDecimal scale2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}
