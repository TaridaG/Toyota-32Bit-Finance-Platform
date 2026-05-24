package com.company.notification.watchlist.infrastructure.kafka;

import com.company.notification.watchlist.application.SyncWatchlistProjectionUseCase;
import com.company.notification.watchlist.infrastructure.kafka.messaging.WatchlistItemAddedMessage;
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
class WatchlistItemAddedEventConsumerTest {

    @Mock
    private SyncWatchlistProjectionUseCase syncWatchlistProjectionUseCase;

    @InjectMocks
    private WatchlistItemAddedEventConsumer consumer;

    @Test
    void consume_delegates_to_projection_use_case() {
        WatchlistItemAddedMessage message = new WatchlistItemAddedMessage();
        message.setEventId(UUID.randomUUID());
        message.setUserId(UUID.randomUUID());
        message.setInstrumentId(1L);
        message.setSymbol("BTCUSDT");
        message.setOccurredAt(Instant.now());
        ConsumerRecord<String, WatchlistItemAddedMessage> record =
                new ConsumerRecord<>("watchlist.item.added", 0, 0L, "key", message);

        consumer.consume(record);

        verify(syncWatchlistProjectionUseCase).applyAdded(message);
    }
}
