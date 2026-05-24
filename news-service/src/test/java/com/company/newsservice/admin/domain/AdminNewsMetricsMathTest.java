package com.company.newsservice.admin.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminNewsMetricsMathTest {

    @Test
    void percentChange_returnsZeroWhenBothZero() {
        assertEquals(0.0, AdminNewsMetricsMath.percentChange(0, 0));
    }

    @Test
    void percentChange_returnsHundredWhenPreviousZeroAndCurrentPositive() {
        assertEquals(100.0, AdminNewsMetricsMath.percentChange(5, 0));
    }

    @Test
    void percentChange_calculatesRelativeDelta() {
        assertEquals(50.0, AdminNewsMetricsMath.percentChange(15, 10));
        assertEquals(-25.0, AdminNewsMetricsMath.percentChange(15, 20));
    }
}
