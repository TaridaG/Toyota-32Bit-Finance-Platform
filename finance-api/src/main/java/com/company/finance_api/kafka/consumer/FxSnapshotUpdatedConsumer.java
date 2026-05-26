package com.company.finance_api.kafka.consumer;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.ProcessedEvent;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.kafka.MarketDataTopics;
import com.company.finance_api.kafka.event.FxSnapshotUpdatedEvent;
import com.company.finance_api.kafka.support.SemanticPriceWriteMetrics;
import com.company.finance_api.kafka.support.SnapshotInstrumentResolution;
import com.company.finance_api.repository.ProcessedEventRepository;
import com.company.finance_api.pricing.application.PriceService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** FX snapshot güncelleme Kafka mesajlarını consume eder. */
@Slf4j
@Component
@Profile("kafka")
@RequiredArgsConstructor
public class FxSnapshotUpdatedConsumer {

  private final SnapshotInstrumentResolution snapshotInstrumentResolution;
  private final PriceService priceService;
  private final ProcessedEventRepository processedEventRepository;
  private final MeterRegistry meterRegistry;
  private final SemanticPriceWriteMetrics semanticPriceWriteMetrics;

  @Transactional
  @KafkaListener(
      topics = MarketDataTopics.MARKET_FX_SNAPSHOT_UPDATED,
      groupId = "finance-api-fx-snapshot-consumer",
      containerFactory = "fxSnapshotKafkaListenerContainerFactory")
  public void consume(ConsumerRecord<String, FxSnapshotUpdatedEvent> record) {
    if (record.value() == null) {
      return;
    }
    FxSnapshotUpdatedEvent event = record.value();
    if (event.eventId() == null || event.eventId().isBlank()) {
      meterRegistry
          .counter(
              "invalid_fx_snapshot_total",
              Tags.of("service", "finance-api", "reason", "missing_event_id"))
          .increment();
      return;
    }
    if (processedEventRepository.existsById(event.eventId())) {
      log.debug("Duplicate FX snapshot skipped eventId={}", event.eventId());
      return;
    }
    Optional<BigDecimal> priceOpt = selectFxPrice(event);
    if (priceOpt.isEmpty()) {
      meterRegistry
          .counter(
              "invalid_fx_snapshot_total",
              Tags.of("service", "finance-api", "reason", "no_positive_price"))
          .increment();
      processedEventRepository.save(new ProcessedEvent(event.eventId()));
      return;
    }
    Optional<Instrument> instrumentOpt = snapshotInstrumentResolution.resolveFx(event);
    if (instrumentOpt.isEmpty()) {
      meterRegistry
          .counter(
              "invalid_fx_snapshot_total",
              Tags.of("service", "finance-api", "reason", "unresolved_instrument"))
          .increment();
      processedEventRepository.save(new ProcessedEvent(event.eventId()));
      return;
    }
    Instant ts = event.occurredAt() != null ? event.occurredAt() : Instant.now();
    InstrumentPrice price =
        InstrumentPrice.builder()
            .instrument(instrumentOpt.get())
            .priceType(PriceType.FX_MID)
            .price(priceOpt.get())
            .timestamp(ts)
            .build();
    priceService.savePrice(price);
    semanticPriceWriteMetrics.record("fx_mid", event.source());
    processedEventRepository.save(new ProcessedEvent(event.eventId()));
    meterRegistry
        .counter("fx_snapshot_consumed_total", Tags.of("service", "finance-api"))
        .increment();
    log.info(
        "FX_SNAPSHOT_APPLIED_TO_PRICE eventId={} instrumentId={} symbol={} price={}",
        event.eventId(),
        instrumentOpt.get().getId(),
        event.canonicalSymbol(),
        priceOpt.get());
  }

  private static Optional<BigDecimal> selectFxPrice(FxSnapshotUpdatedEvent event) {
    if (event.mid() != null && event.mid().compareTo(BigDecimal.ZERO) > 0) {
      return Optional.of(event.mid());
    }
    if (event.bid() != null && event.bid().compareTo(BigDecimal.ZERO) > 0) {
      return Optional.of(event.bid());
    }
    if (event.ask() != null && event.ask().compareTo(BigDecimal.ZERO) > 0) {
      return Optional.of(event.ask());
    }
    return Optional.empty();
  }
}
