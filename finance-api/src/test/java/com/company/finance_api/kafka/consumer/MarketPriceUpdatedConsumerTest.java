package com.company.finance_api.kafka.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.finance_api.domain.Instrument;
import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.ProcessedEvent;
import com.company.finance_api.domain.enums.Exchange;
import com.company.finance_api.domain.enums.InstrumentType;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.kafka.event.MarketPriceUpdatedEvent;
import com.company.finance_api.kafka.support.SemanticPriceWriteMetrics;
import com.company.finance_api.repository.InstrumentRepository;
import com.company.finance_api.repository.ProcessedEventRepository;
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
class MarketPriceUpdatedConsumerTest {

  @Mock private InstrumentRepository instrumentRepository;
  @Mock private PriceService priceService;
  @Mock private ProcessedEventRepository processedEventRepository;
  @Mock private SemanticPriceWriteMetrics semanticPriceWriteMetrics;

  private MarketPriceUpdatedConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer =
        new MarketPriceUpdatedConsumer(
            instrumentRepository,
            priceService,
            processedEventRepository,
            new SimpleMeterRegistry(),
            semanticPriceWriteMetrics);
  }

  @Test
  void consume_skipsDuplicateEvents() {
    MarketPriceUpdatedEvent event = sampleEvent("evt-dup", "BTCUSDT", 1L);
    ConsumerRecord<String, MarketPriceUpdatedEvent> record =
        new ConsumerRecord<>("market-price-updated", 0, 0L, "evt-dup", event);

    when(processedEventRepository.existsById("evt-dup")).thenReturn(true);

    consumer.consume(record);

    verify(priceService, never()).savePrice(any());
    verify(processedEventRepository, never()).save(any());
  }

  @Test
  void consume_skipsUnknownInstrumentButMarksProcessed() {
    MarketPriceUpdatedEvent event = sampleEvent("evt-missing", "UNKNOWN", null);
    ConsumerRecord<String, MarketPriceUpdatedEvent> record =
        new ConsumerRecord<>("market-price-updated", 0, 1L, "evt-missing", event);

    when(processedEventRepository.existsById("evt-missing")).thenReturn(false);
    when(instrumentRepository.findBySymbol("UNKNOWN")).thenReturn(Optional.empty());

    consumer.consume(record);

    verify(priceService, never()).savePrice(any());
    verify(processedEventRepository).save(any(ProcessedEvent.class));
  }

  @Test
  void consume_persistsPriceForKnownInstrument() {
    Instrument instrument =
        new Instrument("BTCUSDT", "Bitcoin", InstrumentType.CRYPTO, Exchange.BINANCE);
    MarketPriceUpdatedEvent event = sampleEvent("evt-ok", "BTCUSDT", null);
    ConsumerRecord<String, MarketPriceUpdatedEvent> record =
        new ConsumerRecord<>("market-price-updated", 0, 2L, "evt-ok", event);

    when(processedEventRepository.existsById("evt-ok")).thenReturn(false);
    when(instrumentRepository.findBySymbol("BTCUSDT")).thenReturn(Optional.of(instrument));

    consumer.consume(record);

    verify(priceService).savePrice(any(InstrumentPrice.class));
    verify(semanticPriceWriteMetrics).record(eq("market_MARKET"), eq("binance"));
    verify(processedEventRepository).save(any(ProcessedEvent.class));
  }

  private static MarketPriceUpdatedEvent sampleEvent(
      String eventId, String symbol, Long instrumentId) {
    return new MarketPriceUpdatedEvent(
        eventId,
        symbol,
        new BigDecimal("42000.50"),
        PriceType.MARKET.name(),
        "binance",
        Instant.parse("2026-05-24T10:00:00Z"),
        instrumentId);
  }
}
