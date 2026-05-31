package com.company.finance_api.outbox.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.outbox.domain.OutboxEvent;
import com.company.finance_api.outbox.domain.enums.OutboxStatus;
import com.company.finance_api.outbox.infrastructure.persistence.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherSchedulerTest {

  @Mock private OutboxEventRepository outboxEventRepository;
  @Mock private KafkaTemplate<String, Object> kafkaTemplate;
  @Mock private SendResult<String, Object> sendResult;

  private OutboxPublisherScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new OutboxPublisherScheduler(outboxEventRepository, kafkaTemplate, new ObjectMapper());
  }

  @Test
  void publish_doesNothingWhenBatchEmpty() {
    when(outboxEventRepository.lockBatch(
            eq(OutboxStatus.NEW),
            eq(OutboxStatus.RETRY),
            any(),
            eq("internal."),
            any(Pageable.class)))
        .thenReturn(List.of());

    scheduler.publish();

    verify(kafkaTemplate, never()).send(any(String.class), any());
  }

  @Test
  void publish_marksEventSentOnSuccess() throws Exception {
    OutboxEvent event =
        OutboxEvent.newEvent("orders", "key-1", "java.lang.String", "\"payload\"");
    when(outboxEventRepository.lockBatch(
            eq(OutboxStatus.NEW),
            eq(OutboxStatus.RETRY),
            any(),
            eq("internal."),
            any(Pageable.class)))
        .thenReturn(List.of(event));
    when(kafkaTemplate.send(eq("orders"), eq("key-1"), eq("payload")))
        .thenReturn(CompletableFuture.completedFuture(sendResult));

    scheduler.publish();

    verify(kafkaTemplate).send("orders", "key-1", "payload");
    org.junit.jupiter.api.Assertions.assertEquals(OutboxStatus.SENT, event.getStatus());
  }

  @Test
  void publish_schedulesRetryOnFailure() {
    OutboxEvent event =
        OutboxEvent.newEvent("orders", null, "java.lang.String", "\"payload\"");
    when(outboxEventRepository.lockBatch(
            eq(OutboxStatus.NEW),
            eq(OutboxStatus.RETRY),
            any(),
            eq("internal."),
            any(Pageable.class)))
        .thenReturn(List.of(event));
    when(kafkaTemplate.send(eq("orders"), eq("payload")))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("broker down")));

    scheduler.publish();

    org.junit.jupiter.api.Assertions.assertEquals(OutboxStatus.RETRY, event.getStatus());
    org.junit.jupiter.api.Assertions.assertEquals(1, event.getAttempts());
    org.junit.jupiter.api.Assertions.assertNotNull(event.getNextAttemptAt());
  }
}
