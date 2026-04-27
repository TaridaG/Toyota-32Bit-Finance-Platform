package com.company.notification.consumer;

import com.company.notification.config.kafka.NotificationKafkaTopics;
import com.company.notification.event.WatchlistItemAddedEvent;
import com.company.notification.service.WatchlistProjectionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WatchlistItemAddedConsumer {

    private final WatchlistProjectionService watchlistProjectionService;

    public WatchlistItemAddedConsumer(WatchlistProjectionService watchlistProjectionService) {
        this.watchlistProjectionService = watchlistProjectionService;
    }

    @KafkaListener(
            topics = NotificationKafkaTopics.WATCHLIST_ITEM_ADDED,
            groupId = "notification-service-watchlist-projection",
            containerFactory = "watchlistItemAddedKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, WatchlistItemAddedEvent> record) {
        watchlistProjectionService.applyAdded(record.value());
    }
}
