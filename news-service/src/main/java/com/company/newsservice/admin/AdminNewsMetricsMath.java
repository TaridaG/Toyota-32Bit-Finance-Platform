package com.company.newsservice.admin;

public final class AdminNewsMetricsMath {

    private AdminNewsMetricsMath() {}

    public static double percentChange(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return 100.0 * (current - previous) / previous;
    }
}
