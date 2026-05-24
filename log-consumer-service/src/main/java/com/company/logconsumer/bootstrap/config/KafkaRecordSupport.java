package com.company.logconsumer.bootstrap.config;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Kafka consumer kayıtlarından metadata çıkaran yardımcılar (DLQ loglama vb.).
 */
public final class KafkaRecordSupport {

    private static final Pattern EVENT_ID_PATTERN =
            Pattern.compile("\"eventId\"\\s*:\\s*\"([^\"]+)\"");

    private KafkaRecordSupport() {
    }

    /**
     * Ham JSON gövdeden {@code eventId} alanını regex ile çıkarır; bulunamazsa {@code unknown}.
     */
    public static String extractEventId(Object value) {
        if (value == null) {
            return "unknown";
        }
        String raw = String.valueOf(value);
        Matcher matcher = EVENT_ID_PATTERN.matcher(raw);
        return matcher.find() ? matcher.group(1) : "unknown";
    }
}
