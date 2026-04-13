package com.company.analytics.application.util;

import com.company.analytics.domain.enums.CandleInterval;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public final class TimeBucketUtil {

    private TimeBucketUtil() {}

    public static Instant truncate(Instant time, CandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> time.truncatedTo(ChronoUnit.MINUTES);

            case FIVE_MINUTES -> {
                long minutes = time.getEpochSecond() / 60;
                long bucket = (minutes / 5) * 5;
                yield Instant.ofEpochSecond(bucket * 60);
            }

            case ONE_HOUR -> time.truncatedTo(ChronoUnit.HOURS);

            case ONE_DAY -> {
                ZonedDateTime utc = time.atZone(ZoneOffset.UTC);
                yield utc.truncatedTo(ChronoUnit.DAYS).toInstant();
            }
        };
    }
}