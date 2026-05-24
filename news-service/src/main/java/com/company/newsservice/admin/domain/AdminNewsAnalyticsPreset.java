package com.company.newsservice.admin.domain;

import java.util.Locale;

/** Admin news analytics için UTC calendar preset'leri (portal user analytics ile aynı semantics). */
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

    /** REST query param değerini döner. */
    public String queryParam() {
        return queryParam;
    }

    /** Preset'in kapsadığı inclusive gün sayısını döner. */
    public int inclusiveDayCount() {
        return inclusiveDayCount;
    }

    /** Ham preset string'ini parse eder; bilinmeyende {@link #LAST_7_DAYS} döner. */
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
