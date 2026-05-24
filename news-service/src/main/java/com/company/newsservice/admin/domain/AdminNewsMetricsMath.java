package com.company.newsservice.admin.domain;

/**
 * Admin news analytics metrikleri için yardımcı matematik fonksiyonları.
 */
public final class AdminNewsMetricsMath {

    private AdminNewsMetricsMath() {}

    /** İki değer arasında yüzde değişim hesaplar. */
    public static double percentChange(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return 100.0 * (current - previous) / previous;
    }
}
