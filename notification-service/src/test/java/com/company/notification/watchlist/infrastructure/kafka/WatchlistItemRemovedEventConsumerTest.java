package com.company.notification.watchlist.infrastructure.kafka;

import com.company.notification.watchlist.application.SyncWatchlistProjectionUseCase;
import com.company.notification.watchlist.infrastructure.kafka.messaging.WatchlistItemRemovedMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WatchlistItemRemovedEventConsumerTest {

    @Mock
    private SyncWatchlistProjectionUseCase syncWatchlistProjectionUseCase;

    @InjectMocks
    private WatchlistItemRemovedEventConsumer consumer;

    @Test
    void consume_delegates_to_projection_use_case() {
        WatchlistItemRemovedMessage message = new WatchlistItemRemovedMessage();
        message.setEventId(UUID.randomUUID());
        message.setUserId(UUID.randomUUID());
        message.setInstrumentId(1L);
        message.setSymbol("BTCUSDT");
        message.setOccurredAt(Instant.now());
        ConsumerRecord<String, WatchlistItemRemovedMessage> record =
                new ConsumerRecord<>("watchlist.item.removed", 0, 0L, "key", message);

        consumer.consume(record);

        verify(syncWatchlistProjectionUseCase).applyRemoved(message);
    }
}
