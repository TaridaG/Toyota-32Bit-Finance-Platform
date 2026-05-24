package com.company.notification.insight.flow;

import com.company.notification.insight.domain.PendingInsightEvent;
import com.company.notification.insight.infrastructure.kafka.SimpleInsightEventConsumer;
import com.company.notification.insight.infrastructure.kafka.messaging.AnalyticsInsightMessage;
import com.company.notification.insight.infrastructure.persistence.PendingInsightEventRepository;
import com.company.notification.insight.infrastructure.scheduler.InsightAggregationScheduler;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimpleInsightFlowTest {

    @Mock
    private SyncWatchlistProjectionUseCase syncWatchlistProjectionUseCase;
    @Mock
    private PendingInsightEventRepository pendingInsightEventRepository;

    @Test
    void event_flow_should_store_and_aggregate_and_mark_processed() {
        SimpleInsightEventConsumer consumer = new SimpleInsightEventConsumer(
                syncWatchlistProjectionUseCase,
                pendingInsightEventRepository,
                new SimpleMeterRegistry()
        );
        InsightAggregationScheduler scheduler = new InsightAggregationScheduler(
                pendingInsightEventRepository,
                new SimpleMeterRegistry()
        );

        WatchlistProjection follower = new WatchlistProjection();
        UUID userId = UUID.randomUUID();
        follower.setUserId(userId);
        follower.setActive(true);
        when(syncWatchlistProjectionUseCase.findActiveFollowers(1L)).thenReturn(List.of(follower));

        AnalyticsInsightMessage insightEvent = new AnalyticsInsightMessage();
        insightEvent.setEventId(UUID.randomUUID());
        insightEvent.setInstrumentId(1L);
        insightEvent.setSymbol("BTC");
        insightEvent.setChangePercent(new BigDecimal("0.092"));
        insightEvent.setDirection("UP");
        insightEvent.setOccurredAt(Instant.now());

        consumer.consume(new ConsumerRecord<>("analytics.insight.simple", 0, 0L, "1", insightEvent));

        ArgumentCaptor<PendingInsightEvent> savedEventCaptor = ArgumentCaptor.forClass(PendingInsightEvent.class);
        verify(pendingInsightEventRepository).save(savedEventCaptor.capture());
        PendingInsightEvent savedPending = savedEventCaptor.getValue();
        assertEquals(false, savedPending.isProcessed());

        when(pendingInsightEventRepository.findByProcessedFalse()).thenReturn(List.of(savedPending));

        scheduler.aggregateAndSend();

        ArgumentCaptor<List<PendingInsightEvent>> saveAllCaptor = ArgumentCaptor.forClass(List.class);
        verify(pendingInsightEventRepository).saveAll(saveAllCaptor.capture());
        assertEquals(true, saveAllCaptor.getValue().get(0).isProcessed());
    }

    @Test
    void duplicate_event_should_be_skipped_gracefully() {
        SimpleInsightEventConsumer consumer = new SimpleInsightEventConsumer(
                syncWatchlistProjectionUseCase,
                pendingInsightEventRepository,
                new SimpleMeterRegistry()
        );
        WatchlistProjection follower = new WatchlistProjection();
        UUID userId = UUID.randomUUID();
        follower.setUserId(userId);
        follower.setActive(true);
        when(syncWatchlistProjectionUseCase.findActiveFollowers(1L)).thenReturn(List.of(follower));
        AnalyticsInsightMessage insightEvent = new AnalyticsInsightMessage();
        insightEvent.setEventId(UUID.randomUUID());
        insightEvent.setInstrumentId(1L);
        insightEvent.setSymbol("BTC");
        insightEvent.setChangePercent(new BigDecimal("0.092"));
        insightEvent.setDirection("UP");
        insightEvent.setOccurredAt(Instant.now());

        when(pendingInsightEventRepository.save(any(PendingInsightEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertDoesNotThrow(() -> consumer.consume(new ConsumerRecord<>("analytics.insight.simple", 0, 0L, "1", insightEvent)));
        assertDoesNotThrow(() -> consumer.consume(new ConsumerRecord<>("analytics.insight.simple", 0, 1L, "1", insightEvent)));
        verify(pendingInsightEventRepository, times(2)).save(any(PendingInsightEvent.class));
    }
}
