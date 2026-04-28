package com.company.notification.consumer;

import com.company.notification.client.FinanceInstrumentLookupService;
import com.company.notification.domain.PendingInsightEvent;
import com.company.notification.domain.WatchlistProjection;
import com.company.notification.event.NewsInstrumentMatchedEvent;
import com.company.notification.repository.PendingInsightEventRepository;
import com.company.notification.service.WatchlistProjectionService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsMatchedConsumerTest {

    @Mock
    private WatchlistProjectionService watchlistProjectionService;

    @Mock
    private PendingInsightEventRepository pendingInsightEventRepository;

    @Mock
    private FinanceInstrumentLookupService financeInstrumentLookupService;

    @Test
    void matched_news_creates_pending_for_followers() {
        NewsMatchedConsumer consumer = new NewsMatchedConsumer(
                watchlistProjectionService,
                pendingInsightEventRepository,
                financeInstrumentLookupService,
                new SimpleMeterRegistry()
        );

        UUID userId = UUID.randomUUID();
        WatchlistProjection follower = new WatchlistProjection();
        follower.setUserId(userId);
        follower.setActive(true);

        when(financeInstrumentLookupService.resolveInstrumentId("BTCUSDT")).thenReturn(Optional.of(99L));
        when(watchlistProjectionService.findActiveFollowers(99L)).thenReturn(List.of(follower));
        when(pendingInsightEventRepository.existsByUserIdAndInstrumentIdAndNewsTitleAndEventType(
                eq(userId), eq(99L), eq("Bitcoin rally"), eq("NEWS")
        )).thenReturn(false);

        NewsInstrumentMatchedEvent ev = new NewsInstrumentMatchedEvent();
        ev.setSymbols(List.of("BTCUSDT"));
        ev.setTitle("Bitcoin rally");
        ev.setSourceName("Test");
        ev.setPublishedAt(Instant.parse("2024-01-01T12:00:00Z"));

        consumer.consume(new ConsumerRecord<>("news.instrument.matched", 0, 0L, "k", ev));

        ArgumentCaptor<PendingInsightEvent> cap = ArgumentCaptor.forClass(PendingInsightEvent.class);
        verify(pendingInsightEventRepository).save(cap.capture());
        PendingInsightEvent saved = cap.getValue();
        assertEquals("NEWS", saved.getEventType());
        assertEquals("Bitcoin rally", saved.getNewsTitle());
        assertEquals("NONE", saved.getDirection());
        assertNull(saved.getChangePercent());
        assertEquals(99L, saved.getInstrumentId());
        assertEquals(userId, saved.getUserId());
    }

    @Test
    void no_watchlist_skips_save() {
        NewsMatchedConsumer consumer = new NewsMatchedConsumer(
                watchlistProjectionService,
                pendingInsightEventRepository,
                financeInstrumentLookupService,
                new SimpleMeterRegistry()
        );
        when(financeInstrumentLookupService.resolveInstrumentId("X")).thenReturn(Optional.of(1L));
        when(watchlistProjectionService.findActiveFollowers(1L)).thenReturn(List.of());

        NewsInstrumentMatchedEvent ev = new NewsInstrumentMatchedEvent();
        ev.setSymbols(List.of("X"));
        ev.setTitle("T");
        ev.setPublishedAt(Instant.now());

        consumer.consume(new ConsumerRecord<>("news.instrument.matched", 0, 0L, "k", ev));

        verify(pendingInsightEventRepository, never()).save(any());
    }

    @Test
    void duplicate_title_user_instrument_skips_second_save() {
        NewsMatchedConsumer consumer = new NewsMatchedConsumer(
                watchlistProjectionService,
                pendingInsightEventRepository,
                financeInstrumentLookupService,
                new SimpleMeterRegistry()
        );
        UUID userId = UUID.randomUUID();
        WatchlistProjection follower = new WatchlistProjection();
        follower.setUserId(userId);
        when(financeInstrumentLookupService.resolveInstrumentId("ETHUSDT")).thenReturn(Optional.of(2L));
        when(watchlistProjectionService.findActiveFollowers(2L)).thenReturn(List.of(follower));
        when(pendingInsightEventRepository.existsByUserIdAndInstrumentIdAndNewsTitleAndEventType(
                eq(userId), eq(2L), eq("Same title"), eq("NEWS")
        )).thenReturn(true);

        NewsInstrumentMatchedEvent ev = new NewsInstrumentMatchedEvent();
        ev.setSymbols(List.of("ETHUSDT"));
        ev.setTitle("Same title");
        ev.setPublishedAt(Instant.now());

        consumer.consume(new ConsumerRecord<>("news.instrument.matched", 0, 0L, "k", ev));

        verify(pendingInsightEventRepository, never()).save(any());
    }

    @Test
    void constraint_violation_skips_gracefully() {
        NewsMatchedConsumer consumer = new NewsMatchedConsumer(
                watchlistProjectionService,
                pendingInsightEventRepository,
                financeInstrumentLookupService,
                new SimpleMeterRegistry()
        );
        UUID userId = UUID.randomUUID();
        WatchlistProjection follower = new WatchlistProjection();
        follower.setUserId(userId);
        when(financeInstrumentLookupService.resolveInstrumentId("A")).thenReturn(Optional.of(3L));
        when(watchlistProjectionService.findActiveFollowers(3L)).thenReturn(List.of(follower));
        when(pendingInsightEventRepository.existsByUserIdAndInstrumentIdAndNewsTitleAndEventType(any(), any(), any(), any()))
                .thenReturn(false);
        when(pendingInsightEventRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

        NewsInstrumentMatchedEvent ev = new NewsInstrumentMatchedEvent();
        ev.setSymbols(List.of("A"));
        ev.setTitle("Race");
        ev.setPublishedAt(Instant.now());

        consumer.consume(new ConsumerRecord<>("news.instrument.matched", 0, 0L, "k", ev));

        verify(pendingInsightEventRepository, times(1)).save(any());
    }
}
