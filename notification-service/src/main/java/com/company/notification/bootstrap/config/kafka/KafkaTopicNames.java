package com.company.notification.bootstrap.config.kafka;

/**
 * Bu servisin consume ettiği veya produce ettiği Kafka topic adları.
 */
public final class KafkaTopicNames {

    private KafkaTopicNames() {
    }

    public static final String ALARM_TRIGGERED = "alarm-triggered";
    public static final String LOGIN_SECURITY_ALERT = "login-security.alert";
    public static final String REPORT_COMPLETED = "report.completed";
    public static final String REPORT_FAILED = "report.failed";
    public static final String WATCHLIST_ITEM_ADDED = "watchlist.item.added";
    public static final String WATCHLIST_ITEM_REMOVED = "watchlist.item.removed";
    public static final String ANALYTICS_INSIGHT_SIMPLE = "analytics.insight.simple";
    public static final String NEWS_INSTRUMENT_MATCHED = "news.instrument.matched";
}
