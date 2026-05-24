package com.company.analytics.processing.application;

import com.company.analytics.processing.domain.ProcessedAnalyticsEvent;
import com.company.analytics.processing.infrastructure.persistence.ProcessedAnalyticsEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventIdempotencyServiceImplTest {

    @Mock
    private ProcessedAnalyticsEventRepository repository;

    @InjectMocks
    private EventIdempotencyServiceImpl service;

    @Test
    void isProcessed_returnsTrueWhenEventKeyExists() {
        when(repository.findByEventKey("evt-1")).thenReturn(Optional.of(new ProcessedAnalyticsEvent()));

        assertTrue(service.isProcessed("evt-1"));
    }

    @Test
    void isProcessed_returnsFalseWhenEventKeyMissing() {
        when(repository.findByEventKey("evt-2")).thenReturn(Optional.empty());

        assertFalse(service.isProcessed("evt-2"));
    }

    @Test
    void markProcessed_persistsEventKey() {
        service.markProcessed("evt-3");

        ArgumentCaptor<ProcessedAnalyticsEvent> captor = ArgumentCaptor.forClass(ProcessedAnalyticsEvent.class);
        verify(repository).save(captor.capture());
        assertEquals("evt-3", captor.getValue().getEventKey());
        assertTrue(captor.getValue().getProcessedAt() != null);
    }
}
