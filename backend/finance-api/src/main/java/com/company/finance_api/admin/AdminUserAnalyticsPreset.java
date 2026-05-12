package com.company.finance_api.admin;

import java.util.Locale;

/**
 * Preset window for admin user analytics charts and period KPIs (UTC calendar alignment).
 */
public enum AdminUserAnalyticsPreset {
    LAST_7_DAYS("7d", 7),
    LAST_14_DAYS("14d", 14),
    LAST_30_DAYS("30d", 30);

    private final String queryParam;
    private final int inclusiveDayCount;

    AdminUserAnalyticsPreset(String queryParam, int inclusiveDayCount) {
        this.queryParam = queryParam;
        this.inclusiveDayCount = inclusiveDayCount;
    }

    public String queryParam() {
        return queryParam;
    }

    /** Number of UTC calendar days in the chart (including today). */
    public int inclusiveDayCount() {
        return inclusiveDayCount;
    }

    public static AdminUserAnalyticsPreset parse(String raw) {
        if (raw == null) {
            return LAST_7_DAYS;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        for (AdminUserAnalyticsPreset p : values()) {
            if (p.queryParam.equals(s)) {
                return p;
            }
        }
        return LAST_7_DAYS;
    }
}
