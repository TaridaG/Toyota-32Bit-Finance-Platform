package com.company.finance_api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.OutboxEvent;
import com.company.finance_api.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxServiceImplTest {

  @Mock OutboxEventRepository outboxEventRepository;
  @Mock ObjectMapper objectMapper;

  OutboxServiceImpl outboxService;

  @BeforeEach
  void setUp() {
    outboxService = new OutboxServiceImpl(outboxEventRepository, objectMapper);
  }

  @Test
  void enqueue_should_persistSerializedOutboxEvent() throws Exception {
    SamplePayload payload = new SamplePayload("watchlist-updated", 7L);
    when(objectMapper.writeValueAsString(payload)).thenReturn("{\"event\":\"watchlist-updated\"}");

    outboxService.enqueue("watchlist.events", "user-123", payload);

    ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
    verify(outboxEventRepository).save(captor.capture());
    OutboxEvent saved = captor.getValue();

    assertEquals("watchlist.events", saved.getTopic());
    assertEquals("user-123", saved.getMessageKey());
    assertEquals(SamplePayload.class.getName(), saved.getEventType());
    assertEquals("{\"event\":\"watchlist-updated\"}", saved.getPayloadJson());
  }

  @Test
  void enqueue_should_failFastWhenSerializationFails() throws Exception {
    SamplePayload payload = new SamplePayload("broken", 1L);
    when(objectMapper.writeValueAsString(payload))
        .thenThrow(new JsonProcessingException("cannot serialize") {});

    IllegalStateException ex =
        assertThrows(
            IllegalStateException.class,
            () -> outboxService.enqueue("watchlist.events", "user-123", payload));

    assertEquals("Outbox payload serialization failed", ex.getMessage());
    verify(outboxEventRepository, never()).save(any(OutboxEvent.class));
  }

  private record SamplePayload(String event, long version) {}
}
