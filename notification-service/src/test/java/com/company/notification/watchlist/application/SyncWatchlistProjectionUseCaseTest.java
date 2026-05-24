package com.company.notification.watchlist.application;

import com.company.notification.watchlist.domain.WatchlistProjection;
import com.company.notification.watchlist.infrastructure.kafka.messaging.WatchlistItemAddedMessage;
import com.company.notification.watchlist.infrastructure.kafka.messaging.WatchlistItemRemovedMessage;
import com.company.notification.watchlist.infrastructure.persistence.NotificationProcessedEventRepository;
import com.company.notification.watchlist.infrastructure.persistence.WatchlistProjectionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncWatchlistProjectionUseCaseTest {

    @Mock
    private WatchlistProjectionRepository watchlistProjectionRepository;
    @Mock
    private NotificationProcessedEventRepository notificationProcessedEventRepository;

    private SyncWatchlistProjectionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SyncWatchlistProjectionUseCase(
                watchlistProjectionRepository,
                notificationProcessedEventRepository,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void added_creates_projection() {
        WatchlistItemAddedMessage event = addedEvent(UUID.randomUUID(), Instant.now());
        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(watchlistProjectionRepository.findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId()))
                .thenReturn(Optional.empty());

        useCase.applyAdded(event);

        verify(watchlistProjectionRepository).save(any(WatchlistProjection.class));
        verify(notificationProcessedEventRepository).save(any());
    }

    @Test
    void added_duplicate_event_skipped() {
        WatchlistItemAddedMessage event = addedEvent(UUID.randomUUID(), Instant.now());
        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(true);

        useCase.applyAdded(event);

        verify(watchlistProjectionRepository, never()).save(any());
        verify(notificationProcessedEventRepository, never()).save(any());
    }

    @Test
    void added_existing_activates_and_updates_symbol() {
        Instant now = Instant.now();
        WatchlistItemAddedMessage event = addedEvent(UUID.randomUUID(), now);
        WatchlistProjection existing = new WatchlistProjection();
        existing.setUserId(event.getUserId());
        existing.setInstrumentId(event.getInstrumentId());
        existing.setSymbol("OLD");
        existing.setActive(false);
        existing.setUpdatedAt(now.minusSeconds(60));

        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(watchlistProjectionRepository.findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId()))
                .thenReturn(Optional.of(existing));

        useCase.applyAdded(event);

        verify(watchlistProjectionRepository).save(existing);
        verify(notificationProcessedEventRepository).save(any());
    }

    @Test
    void removed_deactivates() {
        WatchlistItemRemovedMessage event = removedEvent(UUID.randomUUID(), Instant.now());
        WatchlistProjection existing = new WatchlistProjection();
        existing.setUserId(event.getUserId());
        existing.setInstrumentId(event.getInstrumentId());
        existing.setActive(true);
        existing.setUpdatedAt(event.getOccurredAt().minusSeconds(10));

        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(watchlistProjectionRepository.findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId()))
                .thenReturn(Optional.of(existing));

        useCase.applyRemoved(event);

        verify(watchlistProjectionRepository).save(existing);
        verify(notificationProcessedEventRepository).save(any());
    }

    @Test
    void removed_missing_is_noop() {
        WatchlistItemRemovedMessage event = removedEvent(UUID.randomUUID(), Instant.now());
        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(watchlistProjectionRepository.findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId()))
                .thenReturn(Optional.empty());

        useCase.applyRemoved(event);

        verify(watchlistProjectionRepository, never()).save(any());
        verify(notificationProcessedEventRepository).save(any());
    }

    @Test
    void stale_event_ignored() {
        Instant now = Instant.now();
        WatchlistItemAddedMessage event = addedEvent(UUID.randomUUID(), now.minusSeconds(120));
        WatchlistProjection existing = new WatchlistProjection();
        existing.setUserId(event.getUserId());
        existing.setInstrumentId(event.getInstrumentId());
        existing.setActive(true);
        existing.setUpdatedAt(now);

        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(watchlistProjectionRepository.findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId()))
                .thenReturn(Optional.of(existing));

        useCase.applyAdded(event);

        verify(watchlistProjectionRepository, never()).save(any());
        verify(notificationProcessedEventRepository).save(any());
    }

    @Test
    void find_active_followers_returns_repository_data() {
        when(watchlistProjectionRepository.findByInstrumentIdAndActiveTrue(1L)).thenReturn(List.of(new WatchlistProjection()));
        useCase.findActiveFollowers(1L);
        verify(watchlistProjectionRepository).findByInstrumentIdAndActiveTrue(1L);
    }

    private WatchlistItemAddedMessage addedEvent(UUID eventId, Instant occurredAt) {
        WatchlistItemAddedMessage event = new WatchlistItemAddedMessage();
        event.setEventId(eventId);
        event.setUserId(UUID.randomUUID());
        event.setInstrumentId(1L);
        event.setSymbol("BTCUSDT");
        event.setOccurredAt(occurredAt);
        return event;
    }

    private WatchlistItemRemovedMessage removedEvent(UUID eventId, Instant occurredAt) {
        WatchlistItemRemovedMessage event = new WatchlistItemRemovedMessage();
        event.setEventId(eventId);
        event.setUserId(UUID.randomUUID());
        event.setInstrumentId(1L);
        event.setSymbol("BTCUSDT");
        event.setOccurredAt(occurredAt);
        return event;
    }
}
