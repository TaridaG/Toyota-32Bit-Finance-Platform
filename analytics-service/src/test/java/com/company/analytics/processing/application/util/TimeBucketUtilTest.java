package com.company.analytics.processing.application.util;

import com.company.analytics.processing.domain.enums.CandleInterval;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeBucketUtilTest {

    @Test
    void truncate_oneMinute_alignsToMinuteBoundary() {
        Instant input = LocalDateTime.of(2026, 5, 24, 10, 15, 42).toInstant(ZoneOffset.UTC);

        Instant bucket = TimeBucketUtil.truncate(input, CandleInterval.ONE_MINUTE);

        assertEquals(LocalDateTime.of(2026, 5, 24, 10, 15, 0).toInstant(ZoneOffset.UTC), bucket);
    }

    @Test
    void truncate_fiveMinutes_alignsToFiveMinuteBucket() {
        Instant input = LocalDateTime.of(2026, 5, 24, 10, 17, 0).toInstant(ZoneOffset.UTC);

        Instant bucket = TimeBucketUtil.truncate(input, CandleInterval.FIVE_MINUTES);

        assertEquals(LocalDateTime.of(2026, 5, 24, 10, 15, 0).toInstant(ZoneOffset.UTC), bucket);
    }

    @Test
    void truncate_oneHour_alignsToHourBoundary() {
        Instant input = LocalDateTime.of(2026, 5, 24, 10, 45, 0).toInstant(ZoneOffset.UTC);

        Instant bucket = TimeBucketUtil.truncate(input, CandleInterval.ONE_HOUR);

        assertEquals(LocalDateTime.of(2026, 5, 24, 10, 0, 0).toInstant(ZoneOffset.UTC), bucket);
    }

    @Test
    void truncate_oneDay_alignsToUtcMidnight() {
        Instant input = LocalDateTime.of(2026, 5, 24, 23, 59, 0).toInstant(ZoneOffset.UTC);

        Instant bucket = TimeBucketUtil.truncate(input, CandleInterval.ONE_DAY);

        assertEquals(LocalDateTime.of(2026, 5, 24, 0, 0, 0).toInstant(ZoneOffset.UTC), bucket);
    }
}
