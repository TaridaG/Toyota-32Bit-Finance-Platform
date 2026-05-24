package com.company.notification.insight.infrastructure.kafka;

import com.company.notification.insight.domain.PendingInsightEvent;
import com.company.notification.insight.infrastructure.kafka.messaging.AnalyticsInsightMessage;
import com.company.notification.insight.infrastructure.persistence.PendingInsightEventRepository;
import com.company.notification.watchlist.application.SyncWatchlistProjectionUseCase;
import com.company.notification.watchlist.domain.WatchlistProjection;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleInsightEventConsumerTest {

    @Mock
    private SyncWatchlistProjectionUseCase syncWatchlistProjectionUseCase;
    @Mock
    private PendingInsightEventRepository pendingInsightEventRepository;

    @Test
    void consume_skips_null_or_missing_instrument() {
        SimpleInsightEventConsumer consumer = newConsumer();

        consumer.consume(new ConsumerRecord<>("analytics.insight.simple", 0, 0L, "k", null));

        AnalyticsInsightMessage event = new AnalyticsInsightMessage();
        consumer.consume(new ConsumerRecord<>("analytics.insight.simple", 0, 1L, "k", event));

        verifyNoInteractions(syncWatchlistProjectionUseCase, pendingInsightEventRepository);
    }

    @Test
    void consume_skips_when_no_followers() {
        SimpleInsightEventConsumer consumer = newConsumer();
        AnalyticsInsightMessage event = insightMessage();

        when(syncWatchlistProjectionUseCase.findActiveFollowers(10L)).thenReturn(List.of());

        consumer.consume(record(event));

        verify(pendingInsightEventRepository, never()).save(any());
    }

    @Test
    void consume_persists_insight_for_each_follower() {
        SimpleInsightEventConsumer consumer = newConsumer();
        AnalyticsInsightMessage event = insightMessage();
        event.setInstrumentId(10L);
        event.setSymbol("ETH");

        WatchlistProjection follower = new WatchlistProjection();
        follower.setUserId(UUID.randomUUID());
        when(syncWatchlistProjectionUseCase.findActiveFollowers(10L)).thenReturn(List.of(follower));

        consumer.consume(record(event));

        ArgumentCaptor<PendingInsightEvent> captor = ArgumentCaptor.forClass(PendingInsightEvent.class);
        verify(pendingInsightEventRepository).save(captor.capture());
        PendingInsightEvent saved = captor.getValue();
        assertEquals("INSIGHT", saved.getEventType());
        assertEquals("ETH", saved.getSymbol());
        assertEquals(follower.getUserId(), saved.getUserId());
        assertEquals(false, saved.isProcessed());
    }

    @Test
    void consume_ignores_duplicate_constraint() {
        SimpleInsightEventConsumer consumer = newConsumer();
        AnalyticsInsightMessage event = insightMessage();
        event.setInstrumentId(10L);

        WatchlistProjection follower = new WatchlistProjection();
        follower.setUserId(UUID.randomUUID());
        when(syncWatchlistProjectionUseCase.findActiveFollowers(10L)).thenReturn(List.of(follower));
        when(pendingInsightEventRepository.save(any())).thenThrow(new DataIntegrityViolationException("dup"));

        consumer.consume(record(event));

        verify(pendingInsightEventRepository).save(any());
    }

    private SimpleInsightEventConsumer newConsumer() {
        return new SimpleInsightEventConsumer(
                syncWatchlistProjectionUseCase,
                pendingInsightEventRepository,
                new SimpleMeterRegistry()
        );
    }

    private static ConsumerRecord<String, AnalyticsInsightMessage> record(AnalyticsInsightMessage event) {
        return new ConsumerRecord<>("analytics.insight.simple", 0, 0L, "k", event);
    }

    private static AnalyticsInsightMessage insightMessage() {
        AnalyticsInsightMessage event = new AnalyticsInsightMessage();
        event.setInstrumentId(10L);
        event.setSymbol("BTC");
        event.setChangePercent(new BigDecimal("0.05"));
        event.setDirection("UP");
        event.setOccurredAt(Instant.now());
        return event;
    }
}
