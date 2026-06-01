package com.company.notification.insight.infrastructure.scheduler;

import com.company.notification.insight.application.DeliverWatchlistDigestUseCase;
import com.company.notification.insight.domain.PendingInsightEvent;
import com.company.notification.insight.infrastructure.persistence.PendingInsightEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsightAggregationSchedulerTest {

    @Mock
    private PendingInsightEventRepository pendingInsightEventRepository;

    @Mock
    private DeliverWatchlistDigestUseCase deliverWatchlistDigestUseCase;

    @Test
    void aggregateAndSend_noop_when_nothing_pending() {
        when(pendingInsightEventRepository.findByProcessedFalse()).thenReturn(List.of());

        InsightAggregationScheduler scheduler = new InsightAggregationScheduler(
                pendingInsightEventRepository,
                deliverWatchlistDigestUseCase,
                new SimpleMeterRegistry()
        );
        scheduler.aggregateAndSend();

        verify(deliverWatchlistDigestUseCase, never()).deliver(any(), any());
        verify(pendingInsightEventRepository, never()).saveAll(any());
    }

    @Test
    void aggregateAndSend_marks_all_user_events_processed() {
        UUID userId = UUID.randomUUID();
        PendingInsightEvent low = insight(userId, "AAA", "0.01");
        PendingInsightEvent high = insight(userId, "BBB", "0.50");
        PendingInsightEvent news = news(userId, "CCC", "Headline");

        when(pendingInsightEventRepository.findByProcessedFalse())
                .thenReturn(List.of(low, high, news));

        InsightAggregationScheduler scheduler = new InsightAggregationScheduler(
                pendingInsightEventRepository,
                deliverWatchlistDigestUseCase,
                new SimpleMeterRegistry()
        );
        scheduler.aggregateAndSend();

        verify(deliverWatchlistDigestUseCase).deliver(eq(userId), anyList());
        ArgumentCaptor<List<PendingInsightEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(pendingInsightEventRepository).saveAll(captor.capture());
        List<PendingInsightEvent> saved = captor.getValue();
        assertEquals(3, saved.size());
        assertTrue(saved.stream().allMatch(PendingInsightEvent::isProcessed));
    }

    @Test
    void aggregateAndSend_marks_all_events_processed_even_when_more_than_top_five() {
        UUID userId = UUID.randomUUID();
        List<PendingInsightEvent> events = IntStream.range(0, 6)
                .mapToObj(i -> insight(userId, "SYM" + i, "0.0" + i))
                .toList();

        when(pendingInsightEventRepository.findByProcessedFalse()).thenReturn(events);

        InsightAggregationScheduler scheduler = new InsightAggregationScheduler(
                pendingInsightEventRepository,
                deliverWatchlistDigestUseCase,
                new SimpleMeterRegistry()
        );
        scheduler.aggregateAndSend();

        ArgumentCaptor<List<PendingInsightEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(pendingInsightEventRepository).saveAll(captor.capture());
        assertEquals(6, captor.getValue().size());
        assertTrue(captor.getValue().stream().allMatch(PendingInsightEvent::isProcessed));
    }

    @Test
    void aggregateAndSend_groups_by_user() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        when(pendingInsightEventRepository.findByProcessedFalse())
                .thenReturn(List.of(
                        insight(user1, "A", "0.10"),
                        insight(user2, "B", "0.20")
                ));

        InsightAggregationScheduler scheduler = new InsightAggregationScheduler(
                pendingInsightEventRepository,
                deliverWatchlistDigestUseCase,
                new SimpleMeterRegistry()
        );
        scheduler.aggregateAndSend();

        verify(deliverWatchlistDigestUseCase, times(2)).deliver(any(), any());
        verify(pendingInsightEventRepository, times(2)).saveAll(any());
    }

    @Test
    void aggregateAndSend_continues_when_one_user_batch_fails() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        PendingInsightEvent user1Event = insight(user1, "A", "0.10");
        PendingInsightEvent user2Event = insight(user2, "B", "0.20");
        when(pendingInsightEventRepository.findByProcessedFalse())
                .thenReturn(List.of(user1Event, user2Event));
        doThrow(new IllegalStateException("delivery failed"))
                .when(deliverWatchlistDigestUseCase)
                .deliver(eq(user1), anyList());

        InsightAggregationScheduler scheduler = new InsightAggregationScheduler(
                pendingInsightEventRepository,
                deliverWatchlistDigestUseCase,
                new SimpleMeterRegistry()
        );
        scheduler.aggregateAndSend();

        verify(deliverWatchlistDigestUseCase, times(2)).deliver(any(), any());
        verify(pendingInsightEventRepository, times(1)).saveAll(any());
    }

    private static PendingInsightEvent insight(UUID userId, String symbol, String change) {
        PendingInsightEvent e = new PendingInsightEvent();
        e.setUserId(userId);
        e.setInstrumentId(1L);
        e.setSymbol(symbol);
        e.setChangePercent(new BigDecimal(change));
        e.setDirection("UP");
        e.setOccurredAt(Instant.now());
        e.setProcessed(false);
        e.setEventType("INSIGHT");
        return e;
    }

    private static PendingInsightEvent news(UUID userId, String symbol, String title) {
        PendingInsightEvent e = new PendingInsightEvent();
        e.setUserId(userId);
        e.setInstrumentId(2L);
        e.setSymbol(symbol);
        e.setChangePercent(null);
        e.setDirection("NONE");
        e.setOccurredAt(Instant.now());
        e.setProcessed(false);
        e.setEventType("NEWS");
        e.setNewsTitle(title);
        return e;
    }
}
