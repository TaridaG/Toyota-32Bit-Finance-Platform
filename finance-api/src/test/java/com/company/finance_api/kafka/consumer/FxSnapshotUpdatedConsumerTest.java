package com.company.finance_api.shared.messaging.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.outbox.domain.ProcessedEvent;
import com.company.finance_api.instrument.domain.enums.Exchange;
import com.company.finance_api.instrument.domain.enums.InstrumentType;
import com.company.finance_api.shared.messaging.kafka.event.FxSnapshotUpdatedEvent;
import com.company.finance_api.shared.messaging.kafka.support.SemanticPriceWriteMetrics;
import com.company.finance_api.shared.messaging.kafka.support.SnapshotInstrumentResolution;
import com.company.finance_api.outbox.infrastructure.persistence.ProcessedEventRepository;
import com.company.finance_api.pricing.application.PriceService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FxSnapshotUpdatedConsumerTest {

  @Mock private SnapshotInstrumentResolution snapshotInstrumentResolution;
  @Mock private PriceService priceService;
  @Mock private ProcessedEventRepository processedEventRepository;
  @Mock private SemanticPriceWriteMetrics semanticPriceWriteMetrics;

  private FxSnapshotUpdatedConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer =
        new FxSnapshotUpdatedConsumer(
            snapshotInstrumentResolution,
            priceService,
            processedEventRepository,
            new SimpleMeterRegistry(),
            semanticPriceWriteMetrics);
  }

  @Test
  void consume_appliesFxMidPrice() {
    Instrument instrument =
        new Instrument("USDTRY", "USD/TRY", InstrumentType.FX, Exchange.TCMB);
    FxSnapshotUpdatedEvent event =
        new FxSnapshotUpdatedEvent(
            "fx-evt-1",
            "USDTRY",
            null,
            "USD",
            "TRY",
            new BigDecimal("32.10"),
            new BigDecimal("32.20"),
            new BigDecimal("32.15"),
            Instant.parse("2026-05-24T10:00:00Z"),
            "market-data");
    ConsumerRecord<String, FxSnapshotUpdatedEvent> record =
        new ConsumerRecord<>("fx-snapshot-updated", 0, 0L, "fx-evt-1", event);

    when(processedEventRepository.existsById("fx-evt-1")).thenReturn(false);
    when(snapshotInstrumentResolution.resolveFx(event)).thenReturn(Optional.of(instrument));

    consumer.consume(record);

    verify(priceService).savePrice(any(InstrumentPrice.class));
    verify(semanticPriceWriteMetrics).record("fx_mid", "market-data");
    verify(processedEventRepository).save(any(ProcessedEvent.class));
  }

  @Test
  void consume_skipsDuplicateEvent() {
    FxSnapshotUpdatedEvent event =
        new FxSnapshotUpdatedEvent(
            "fx-dup",
            "USDTRY",
            null,
            "USD",
            "TRY",
            null,
            null,
            new BigDecimal("32.15"),
            Instant.now(),
            "market-data");
    ConsumerRecord<String, FxSnapshotUpdatedEvent> record =
        new ConsumerRecord<>("fx-snapshot-updated", 0, 1L, "fx-dup", event);

    when(processedEventRepository.existsById("fx-dup")).thenReturn(true);

    consumer.consume(record);

    verify(priceService, never()).savePrice(any());
    verify(snapshotInstrumentResolution, never()).resolveFx(any());
  }
}
