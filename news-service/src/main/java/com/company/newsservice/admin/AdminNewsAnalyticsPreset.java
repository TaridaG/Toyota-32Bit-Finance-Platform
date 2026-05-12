package com.company.newsservice.admin;

import java.util.Locale;

/** UTC calendar presets for admin news analytics (same semantics as portal user analytics). */
public enum AdminNewsAnalyticsPreset {
    LAST_7_DAYS("7d", 7),
    LAST_14_DAYS("14d", 14),
    LAST_30_DAYS("30d", 30);

    private final String queryParam;
    private final int inclusiveDayCount;

    AdminNewsAnalyticsPreset(String queryParam, int inclusiveDayCount) {
        this.queryParam = queryParam;
        this.inclusiveDayCount = inclusiveDayCount;
    }

    public String queryParam() {
        return queryParam;
    }

    public int inclusiveDayCount() {
        return inclusiveDayCount;
    }

    public static AdminNewsAnalyticsPreset parse(String raw) {
        if (raw == null) {
            return LAST_7_DAYS;
        }
        String s = raw.trim().toLowerCase(Locale.ROOT);
        for (AdminNewsAnalyticsPreset p : values()) {
            if (p.queryParam.equals(s)) {
                return p;
            }
        }
        return LAST_7_DAYS;
    }
}
