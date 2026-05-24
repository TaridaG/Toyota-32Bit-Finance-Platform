package com.company.notification.watchlist.infrastructure.kafka;

import com.company.notification.bootstrap.config.kafka.KafkaTopicNames;
import com.company.notification.watchlist.infrastructure.kafka.messaging.WatchlistItemRemovedMessage;
import com.company.notification.watchlist.application.SyncWatchlistProjectionUseCase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * {@code watchlist.item.removed} event'lerini yerel projection'a uygular.
 */
@Component
public class WatchlistItemRemovedEventConsumer {

    private final SyncWatchlistProjectionUseCase syncWatchlistProjectionUseCase;

    public WatchlistItemRemovedEventConsumer(SyncWatchlistProjectionUseCase syncWatchlistProjectionUseCase) {
        this.syncWatchlistProjectionUseCase = syncWatchlistProjectionUseCase;
    }

    /**
     * Event'i {@link SyncWatchlistProjectionUseCase#applyRemoved} metoduna iletir.
     */
    @KafkaListener(
            topics = KafkaTopicNames.WATCHLIST_ITEM_REMOVED,
            groupId = "notification-service-watchlist-projection",
            containerFactory = "watchlistItemRemovedKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, WatchlistItemRemovedMessage> record) {
        syncWatchlistProjectionUseCase.applyRemoved(record.value());
    }
}
