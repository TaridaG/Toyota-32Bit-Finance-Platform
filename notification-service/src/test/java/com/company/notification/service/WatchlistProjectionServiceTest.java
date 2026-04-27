package com.company.notification.service;

import com.company.notification.domain.WatchlistProjection;
import com.company.notification.event.WatchlistItemAddedEvent;
import com.company.notification.event.WatchlistItemRemovedEvent;
import com.company.notification.repository.NotificationProcessedEventRepository;
import com.company.notification.repository.WatchlistProjectionRepository;
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
class WatchlistProjectionServiceTest {

    @Mock
    private WatchlistProjectionRepository watchlistProjectionRepository;
    @Mock
    private NotificationProcessedEventRepository notificationProcessedEventRepository;

    private WatchlistProjectionService service;

    @BeforeEach
    void setUp() {
        service = new WatchlistProjectionService(
                watchlistProjectionRepository,
                notificationProcessedEventRepository,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void added_creates_projection() {
        WatchlistItemAddedEvent event = addedEvent(UUID.randomUUID(), Instant.now());
        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(watchlistProjectionRepository.findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId()))
                .thenReturn(Optional.empty());

        service.applyAdded(event);

        verify(watchlistProjectionRepository).save(any(WatchlistProjection.class));
        verify(notificationProcessedEventRepository).save(any());
    }

    @Test
    void added_duplicate_event_skipped() {
        WatchlistItemAddedEvent event = addedEvent(UUID.randomUUID(), Instant.now());
        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(true);

        service.applyAdded(event);

        verify(watchlistProjectionRepository, never()).save(any());
        verify(notificationProcessedEventRepository, never()).save(any());
    }

    @Test
    void added_existing_activates_and_updates_symbol() {
        Instant now = Instant.now();
        WatchlistItemAddedEvent event = addedEvent(UUID.randomUUID(), now);
        WatchlistProjection existing = new WatchlistProjection();
        existing.setUserId(event.getUserId());
        existing.setInstrumentId(event.getInstrumentId());
        existing.setSymbol("OLD");
        existing.setActive(false);
        existing.setUpdatedAt(now.minusSeconds(60));

        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(watchlistProjectionRepository.findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId()))
                .thenReturn(Optional.of(existing));

        service.applyAdded(event);

        verify(watchlistProjectionRepository).save(existing);
        verify(notificationProcessedEventRepository).save(any());
    }

    @Test
    void removed_deactivates() {
        WatchlistItemRemovedEvent event = removedEvent(UUID.randomUUID(), Instant.now());
        WatchlistProjection existing = new WatchlistProjection();
        existing.setUserId(event.getUserId());
        existing.setInstrumentId(event.getInstrumentId());
        existing.setActive(true);
        existing.setUpdatedAt(event.getOccurredAt().minusSeconds(10));

        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(watchlistProjectionRepository.findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId()))
                .thenReturn(Optional.of(existing));

        service.applyRemoved(event);

        verify(watchlistProjectionRepository).save(existing);
        verify(notificationProcessedEventRepository).save(any());
    }

    @Test
    void removed_missing_is_noop() {
        WatchlistItemRemovedEvent event = removedEvent(UUID.randomUUID(), Instant.now());
        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(watchlistProjectionRepository.findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId()))
                .thenReturn(Optional.empty());

        service.applyRemoved(event);

        verify(watchlistProjectionRepository, never()).save(any());
        verify(notificationProcessedEventRepository).save(any());
    }

    @Test
    void stale_event_ignored() {
        Instant now = Instant.now();
        WatchlistItemAddedEvent event = addedEvent(UUID.randomUUID(), now.minusSeconds(120));
        WatchlistProjection existing = new WatchlistProjection();
        existing.setUserId(event.getUserId());
        existing.setInstrumentId(event.getInstrumentId());
        existing.setActive(true);
        existing.setUpdatedAt(now);

        when(notificationProcessedEventRepository.existsById(event.getEventId())).thenReturn(false);
        when(watchlistProjectionRepository.findByUserIdAndInstrumentId(event.getUserId(), event.getInstrumentId()))
                .thenReturn(Optional.of(existing));

        service.applyAdded(event);

        verify(watchlistProjectionRepository, never()).save(any());
        verify(notificationProcessedEventRepository).save(any());
    }

    @Test
    void find_active_followers_returns_repository_data() {
        when(watchlistProjectionRepository.findByInstrumentIdAndActiveTrue(1L)).thenReturn(List.of(new WatchlistProjection()));
        service.findActiveFollowers(1L);
        verify(watchlistProjectionRepository).findByInstrumentIdAndActiveTrue(1L);
    }

    private WatchlistItemAddedEvent addedEvent(UUID eventId, Instant occurredAt) {
        WatchlistItemAddedEvent event = new WatchlistItemAddedEvent();
        event.setEventId(eventId);
        event.setUserId(UUID.randomUUID());
        event.setInstrumentId(1L);
        event.setSymbol("BTCUSDT");
        event.setOccurredAt(occurredAt);
        return event;
    }

    private WatchlistItemRemovedEvent removedEvent(UUID eventId, Instant occurredAt) {
        WatchlistItemRemovedEvent event = new WatchlistItemRemovedEvent();
        event.setEventId(eventId);
        event.setUserId(UUID.randomUUID());
        event.setInstrumentId(1L);
        event.setSymbol("BTCUSDT");
        event.setOccurredAt(occurredAt);
        return event;
    }
}
