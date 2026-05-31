package com.company.marketdataservice.bond.domain;
import com.company.marketdataservice.shared.provider.tcmb.BondEodPoint;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Tahvil geçmiş serisinde takvim boşluklarını forward-fill ile doldurur.
 */
public final class BondHistoryForwardFill {

    private BondHistoryForwardFill() {}

    /**
     * @param sparse        raw EVDS points (any order); same calendar day keeps the last occurrence
     * @param rangeStart    inclusive calendar start
     * @param rangeEnd      inclusive calendar end
     * @param seedBeforeRange last known value strictly before {@code rangeStart}, or {@code null}
     */
    public static List<BondEodPoint> expand(
            List<BondEodPoint> sparse,
            LocalDate rangeStart,
            LocalDate rangeEnd,
            BigDecimal seedBeforeRange
    ) {
        if (rangeEnd.isBefore(rangeStart)) {
            return List.of();
        }
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>();
        if (sparse != null) {
            for (BondEodPoint p : sparse) {
                if (p == null || p.date() == null || p.value() == null) {
                    continue;
                }
                byDay.put(p.date(), p.value());
            }
        }
        BigDecimal last = seedBeforeRange;
        List<BondEodPoint> out = new ArrayList<>();
        for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
            BigDecimal v = byDay.get(d);
            if (v != null) {
                last = v;
            }
            if (last != null) {
                out.add(new BondEodPoint(d, last));
            }
        }
        return out;
    }
}
