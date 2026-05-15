package com.company.finance_api.portfolio;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Aligns a calendar-day acquisition instant with end-of-day UTC so MDS/FX
 * "at or before" queries include the full trading day when the UI sends midnight UTC.
 */
public final class FxHistoricalAnchor {

    private FxHistoricalAnchor() {
    }

    /**
     * If {@code acquiredAt} is exactly midnight UTC, returns last nanosecond of that UTC day;
     * otherwise returns {@code acquiredAt} unchanged.
     */
    public static Instant normalizeEndOfAcquisitionDay(Instant acquiredAt) {
        ZonedDateTime utc = acquiredAt.atZone(ZoneOffset.UTC);
        if (utc.getHour() == 0 && utc.getMinute() == 0 && utc.getSecond() == 0 && utc.getNano() == 0) {
            return utc.plusDays(1).minusNanos(1).toInstant();
        }
        return acquiredAt;
    }
}
