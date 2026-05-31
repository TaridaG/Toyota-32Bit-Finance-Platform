package com.company.finance_api.shared.messaging.kafka.consumer;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.outbox.domain.ProcessedEvent;
import com.company.finance_api.pricing.domain.enums.PriceType;
import com.company.finance_api.shared.messaging.kafka.MarketDataTopics;
import com.company.finance_api.shared.messaging.kafka.event.FundSnapshotUpdatedEvent;
import com.company.finance_api.shared.messaging.kafka.support.SemanticPriceWriteMetrics;
import com.company.finance_api.shared.messaging.kafka.support.SnapshotInstrumentResolution;
import com.company.finance_api.outbox.infrastructure.persistence.ProcessedEventRepository;
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

/** Fon snapshot güncelleme Kafka mesajlarını consume eder. */
@Slf4j
@Component
@Profile("kafka")
@RequiredArgsConstructor
public class FundSnapshotUpdatedConsumer {

  private final SnapshotInstrumentResolution snapshotInstrumentResolution;
  private final PriceService priceService;
  private final ProcessedEventRepository processedEventRepository;
  private final MeterRegistry meterRegistry;
  private final SemanticPriceWriteMetrics semanticPriceWriteMetrics;

  @Transactional
  @KafkaListener(
      topics = MarketDataTopics.MARKET_FUND_SNAPSHOT_UPDATED,
      groupId = "finance-api-fund-snapshot-consumer",
      containerFactory = "fundSnapshotKafkaListenerContainerFactory")
  public void consume(ConsumerRecord<String, FundSnapshotUpdatedEvent> record) {
    if (record.value() == null) {
      return;
    }
    FundSnapshotUpdatedEvent event = record.value();
    if (event.eventId() == null || event.eventId().isBlank()) {
      meterRegistry
          .counter(
              "invalid_fund_snapshot_total",
              Tags.of("service", "finance-api", "reason", "missing_event_id"))
          .increment();
      return;
    }
    if (processedEventRepository.existsById(event.eventId())) {
      log.debug("Duplicate fund snapshot skipped eventId={}", event.eventId());
      return;
    }
    if (event.nav() == null || event.nav().compareTo(BigDecimal.ZERO) <= 0) {
      meterRegistry
          .counter(
              "invalid_fund_snapshot_total",
              Tags.of("service", "finance-api", "reason", "invalid_nav"))
          .increment();
      processedEventRepository.save(new ProcessedEvent(event.eventId()));
      return;
    }
    Optional<Instrument> instrumentOpt = snapshotInstrumentResolution.resolveFund(event);
    if (instrumentOpt.isEmpty()) {
      meterRegistry
          .counter(
              "invalid_fund_snapshot_total",
              Tags.of("service", "finance-api", "reason", "unresolved_instrument"))
          .increment();
      processedEventRepository.save(new ProcessedEvent(event.eventId()));
      return;
    }
    Instant ts = event.occurredAt() != null ? event.occurredAt() : Instant.now();
    InstrumentPrice price =
        InstrumentPrice.builder()
            .instrument(instrumentOpt.get())
            .priceType(PriceType.FUND_NAV)
            .price(event.nav())
            .timestamp(ts)
            .build();
    priceService.savePrice(price);
    semanticPriceWriteMetrics.record("fund_nav", event.source());
    processedEventRepository.save(new ProcessedEvent(event.eventId()));
    meterRegistry
        .counter("fund_snapshot_consumed_total", Tags.of("service", "finance-api"))
        .increment();
    log.info(
        "FUND_SNAPSHOT_APPLIED_TO_PRICE eventId={} instrumentId={} fundCode={} nav={}",
        event.eventId(),
        instrumentOpt.get().getId(),
        event.fundCode(),
        event.nav());
  }
}
