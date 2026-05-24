package com.company.analytics.processing.application.util;

import com.company.analytics.processing.domain.enums.CandleInterval;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/** Candle interval'ına göre zaman damgasını bucket başlangıcına yuvarlayan yardımcı sınıf. */
public final class TimeBucketUtil {

    private TimeBucketUtil() {}

    /** Verilen zamanı belirtilen candle interval bucket'ının başlangıcına truncate eder. */
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