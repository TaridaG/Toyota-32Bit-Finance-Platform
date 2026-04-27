package com.company.notification.consumer;

import com.company.notification.config.kafka.NotificationKafkaTopics;
import com.company.notification.event.WatchlistItemRemovedEvent;
import com.company.notification.service.WatchlistProjectionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WatchlistItemRemovedConsumer {

    private final WatchlistProjectionService watchlistProjectionService;

    public WatchlistItemRemovedConsumer(WatchlistProjectionService watchlistProjectionService) {
        this.watchlistProjectionService = watchlistProjectionService;
    }

    @KafkaListener(
            topics = NotificationKafkaTopics.WATCHLIST_ITEM_REMOVED,
            groupId = "notification-service-watchlist-projection",
            containerFactory = "watchlistItemRemovedKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, WatchlistItemRemovedEvent> record) {
        watchlistProjectionService.applyRemoved(record.value());
    }
}
