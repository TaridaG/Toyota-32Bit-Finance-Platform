package com.company.newsservice.admin.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminNewsAnalyticsPresetTest {

    @Test
    void parse_returnsMatchingPreset() {
        assertEquals(AdminNewsAnalyticsPreset.LAST_14_DAYS, AdminNewsAnalyticsPreset.parse("14d"));
        assertEquals(AdminNewsAnalyticsPreset.LAST_30_DAYS, AdminNewsAnalyticsPreset.parse(" 30D "));
    }

    @Test
    void parse_defaultsToSevenDaysWhenUnknownOrNull() {
        assertEquals(AdminNewsAnalyticsPreset.LAST_7_DAYS, AdminNewsAnalyticsPreset.parse(null));
        assertEquals(AdminNewsAnalyticsPreset.LAST_7_DAYS, AdminNewsAnalyticsPreset.parse("invalid"));
    }

    @Test
    void inclusiveDayCount_matchesPreset() {
        assertEquals(7, AdminNewsAnalyticsPreset.LAST_7_DAYS.inclusiveDayCount());
        assertEquals("30d", AdminNewsAnalyticsPreset.LAST_30_DAYS.queryParam());
    }
}
