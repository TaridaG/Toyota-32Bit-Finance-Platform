package com.company.finance_api.event.kafka;

public final class KafkaTopics {

    private KafkaTopics() {}

    public static final String ALARM_TRIGGERED = "alarm-triggered";
    public static final String TRANSACTION_EXECUTED = "transaction-executed";
    public static final String WATCHLIST_ITEM_ADDED = "watchlist.item.added";
    public static final String WATCHLIST_ITEM_REMOVED = "watchlist.item.removed";
}
