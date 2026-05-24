package com.company.notification.watchlist.infrastructure.kafka;

import com.company.notification.bootstrap.config.kafka.KafkaTopicNames;
import com.company.notification.watchlist.infrastructure.kafka.messaging.WatchlistItemAddedMessage;
import com.company.notification.watchlist.application.SyncWatchlistProjectionUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * {@code watchlist.item.added} event'lerini yerel projection'a uygular.
 */
@Component
public class WatchlistItemAddedEventConsumer {

    private final SyncWatchlistProjectionUseCase syncWatchlistProjectionUseCase;

    public WatchlistItemAddedEventConsumer(SyncWatchlistProjectionUseCase syncWatchlistProjectionUseCase) {
        this.syncWatchlistProjectionUseCase = syncWatchlistProjectionUseCase;
    }

    /**
     * Event'i {@link SyncWatchlistProjectionUseCase#applyAdded} metoduna iletir.
     */
    @KafkaListener(
            topics = KafkaTopicNames.WATCHLIST_ITEM_ADDED,
            groupId = "notification-service-watchlist-projection",
            containerFactory = "watchlistItemAddedKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, WatchlistItemAddedMessage> record) {
        syncWatchlistProjectionUseCase.applyAdded(record.value());
    }
}
