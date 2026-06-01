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
     * EVDS'ten gelen sparse günlük noktaları {@code [rangeStart, rangeEnd]} aralığında forward-fill ile yoğunlaştırır.
     *
     * @param sparse seyrek EVDS noktaları (sıra fark etmez); aynı takvim gününde son değer kalır
     * @param rangeStart dahil aralık başlangıcı
     * @param rangeEnd dahil aralık sonu
     * @param seedBeforeRange {@code rangeStart} öncesi son bilinen değer veya {@code null}
     * @return günlük {@link BondEodPoint} listesi
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
