package com.company.finance_api.shared.messaging.kafka.consumer;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.outbox.domain.ProcessedEvent;
import com.company.finance_api.pricing.domain.enums.PriceType;
import com.company.finance_api.shared.messaging.kafka.MarketDataTopics;
import com.company.finance_api.shared.messaging.kafka.event.MarketPriceUpdatedEvent;
import com.company.finance_api.shared.messaging.kafka.support.SemanticPriceWriteMetrics;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
import com.company.finance_api.outbox.infrastructure.persistence.ProcessedEventRepository;
import com.company.finance_api.pricing.application.PriceService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * market-data-service'ten gelen fiyat güncelleme Kafka mesajlarını consume eder.
 */
@Slf4j
@Component
@Profile("kafka")
public class MarketPriceUpdatedConsumer {

    private static final int ALERT_WINDOW_SEC = 60;
    private static final Pattern JSON_EVENT_ID = Pattern.compile("\"eventId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSON_INSTRUMENT_SYMBOL = Pattern.compile("\"instrumentSymbol\"\\s*:\\s*\"([^\"]+)\"");
    private final InstrumentRepository instrumentRepository;
    private final PriceService priceService;
    private final ProcessedEventRepository processedEventRepository;
    private final MeterRegistry meterRegistry;
    private final SemanticPriceWriteMetrics semanticPriceWriteMetrics;
    private final Set<String> distinctMissingSymbols = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, ConcurrentLinkedDeque<Instant>> missingInstrumentEvents = new ConcurrentHashMap<>();
    private final Set<String> priceEventLegacySymbolWarned = ConcurrentHashMap.newKeySet();
    private final Set<String> priceEventInvalidInstrumentIdWarned = ConcurrentHashMap.newKeySet();
    private final Set<String> dataQualityAlertKeys = ConcurrentHashMap.newKeySet();
    private final Set<String> instrumentSymbolMismatchWarned = ConcurrentHashMap.newKeySet();
    private final Set<String> priceEventWithInstrumentIdLogged = ConcurrentHashMap.newKeySet();
    private final AtomicLong priceEventsTotal = new AtomicLong();
    private final AtomicLong priceEventsWithInstrumentIdField = new AtomicLong();

    public MarketPriceUpdatedConsumer(InstrumentRepository instrumentRepository, PriceService priceService, ProcessedEventRepository processedEventRepository, MeterRegistry meterRegistry, SemanticPriceWriteMetrics semanticPriceWriteMetrics) {
        this.instrumentRepository = instrumentRepository;
        this.priceService = priceService;
        this.processedEventRepository = processedEventRepository;
        this.meterRegistry = meterRegistry;
        this.semanticPriceWriteMetrics = semanticPriceWriteMetrics;
        meterRegistry.gauge("finance_kafka_missing_instrument_distinct", distinctMissingSymbols, Set::size);
        Gauge.builder("instrument_id_coverage_ratio", this, c -> {
            long total = c.priceEventsTotal.get();
            return total == 0 ? 0.0 : c.priceEventsWithInstrumentIdField.get() / (double) total;
        }).tag("service", "finance-api").register(meterRegistry);
    }

    @KafkaListener(topics = MarketDataTopics.MARKET_PRICE_UPDATED, groupId = "finance-api-market-price-consumer", containerFactory = "marketPriceKafkaListenerContainerFactory")
    @Transactional
    public void consume(ConsumerRecord<String, MarketPriceUpdatedEvent> record) {
        if (record.value() == null) {
            onDeserializationFailed(record);
            return;
        }
        MarketPriceUpdatedEvent event = record.value();
        if (processedEventRepository.existsById(event.eventId())) {
            log.info("Duplicate event ignored: {}", event.eventId());
            return;
        }
        recordCoverage(event);
        Instrument instrument = resolveInstrument(event);
        if (instrument == null) {
            handleMissingInstrument(event);
            return;
        }

        PriceType priceType = PriceType.valueOf(event.priceType());
        InstrumentPrice price = InstrumentPrice.builder().instrument(instrument).priceType(priceType).price(event.price()).timestamp(event.occurredAt()).build();
        priceService.savePrice(price);
        semanticPriceWriteMetrics.record("market_" + event.priceType().trim(), event.source());
        processedEventRepository.save(new ProcessedEvent(event.eventId()));
    }

    private Instrument resolveInstrument(MarketPriceUpdatedEvent event) {
        Long instrumentId = event.instrumentId();
        if (instrumentId != null) {
            Optional<Instrument> byId = instrumentRepository.findById(instrumentId);
            if (byId.isPresent()) {
                Instrument candidate = byId.get();
                if (candidate.isActive()) {
                    validateInstrumentSymbolMatchesEvent(candidate, event);
                    if (priceEventWithInstrumentIdLogged.add("id:" + instrumentId)) {
                        log.info("PRICE_EVENT_WITH_INSTRUMENT_ID service=finance-api instrumentId={} symbol={} eventId={}", instrumentId, event.instrumentSymbol(), event.eventId());
                    } else {
                        log.debug("PRICE_EVENT_WITH_INSTRUMENT_ID service=finance-api instrumentId={} symbol={} eventId={}", instrumentId, event.instrumentSymbol(), event.eventId());
                    }
                    return candidate;
                }
                faultInvalidInstrumentId(instrumentId, event, "inactive");
            } else {
                faultInvalidInstrumentId(instrumentId, event, "not_found");
            }
        } else {

            meterRegistry.counter("finance_price_event_without_instrument_id_total", "service", "finance-api", "symbol", event.instrumentSymbol() == null ? "unknown" : event.instrumentSymbol(), "instrumentId", "n/a", "reason", "instrument_id_absent").increment();
            String legacyKey = event.instrumentSymbol() == null ? "unknown" : event.instrumentSymbol();
            if (priceEventLegacySymbolWarned.add(legacyKey)) {
                log.warn("PRICE_EVENT_LEGACY_SYMBOL service=finance-api symbol={} eventId={}", event.instrumentSymbol(), event.eventId());
            }
        }
        return instrumentRepository.findBySymbol(event.instrumentSymbol()).orElse(null);
    }

    private void faultInvalidInstrumentId(long instrumentId, MarketPriceUpdatedEvent event, String reason) {

        String symbolTag = event.instrumentSymbol() == null ? "unknown" : event.instrumentSymbol();

        meterRegistry.counter("finance_price_event_invalid_instrument_id_total", "service", "finance-api", "reason", reason, "symbol", symbolTag, "instrumentId", String.valueOf(instrumentId)).increment();

        meterRegistry.counter("invalid_instrument_reference_total", "service", "finance-api", "reason", reason).increment();

        String key = instrumentId + "|" + reason;

        if (priceEventInvalidInstrumentIdWarned.add(key)) {

            log.warn("PRICE_EVENT_INVALID_INSTRUMENT_ID service=finance-api instrumentId={} reason={} symbol={} eventId={}", instrumentId, reason, event.instrumentSymbol(), event.eventId());
        }

        if (dataQualityAlertKeys.add("invalid_ref|finance-api|" + instrumentId + "|" + reason)) {

            log.warn("DATA_QUALITY_ALERT service=finance-api kind=invalid_instrument_reference instrumentId={} reason={} symbol={} eventId={}", instrumentId, reason, event.instrumentSymbol(), event.eventId());
        }
    }

    private void recordCoverage(MarketPriceUpdatedEvent event) {

        priceEventsTotal.incrementAndGet();

        if (event.instrumentId() != null) {

            priceEventsWithInstrumentIdField.incrementAndGet();
        }
    }

    private void validateInstrumentSymbolMatchesEvent(Instrument instrument, MarketPriceUpdatedEvent event) {

        if (event.instrumentId() == null) {

            return;
        }

        String eventSymbol = event.instrumentSymbol();

        if (eventSymbol == null || eventSymbol.isBlank()) {

            return;
        }

        if (eventSymbol.equals(instrument.getSymbol())) {

            return;
        }

        meterRegistry.counter("instrument_symbol_mismatch_total", "service", "finance-api", "reason", "symbol_mismatch").increment();

        String key = "mismatch|finance-api|" + event.instrumentId() + "|" + eventSymbol;

        if (instrumentSymbolMismatchWarned.add(key)) {

            log.warn("instrument_symbol_mismatch_total service=finance-api eventSymbol={} dbSymbol={} instrumentId={} eventId={}", eventSymbol, instrument.getSymbol(), event.instrumentId(), event.eventId());

            log.warn("DATA_QUALITY_ALERT service=finance-api kind=symbol_mismatch eventSymbol={} dbSymbol={} instrumentId={} eventId={}", eventSymbol, instrument.getSymbol(), event.instrumentId(), event.eventId());
        }
    }

    private void onDeserializationFailed(ConsumerRecord<String, MarketPriceUpdatedEvent> record) {

        String eventId = extractEventIdForDeser(record);

        String symbol = extractSymbolForDeser(record);

        if (symbol == null) {

            symbol = "unknown";
        }

        meterRegistry.counter("finance_kafka_market_event_deserialize_errors_total", "service", "finance-api", "symbol", symbol, "reason", "deserialization_error").increment();

        log.warn("malformed_market_event service=finance-api topic={} partition={} offset={} eventId={} reason=deserialization_error", record.topic(), record.partition(), record.offset(), eventId);
    }

    private void handleMissingInstrument(MarketPriceUpdatedEvent event) {
        meterRegistry.counter("finance_kafka_missing_instrument_total", "service", "finance-api", "symbol", event.instrumentSymbol(), "reason", "missing_instrument").increment();
        if (distinctMissingSymbols.add(event.instrumentSymbol())) {
            log.warn("missing_instrument_first_seen service=finance-api symbol={} eventId={}", event.instrumentSymbol(), event.eventId());
        } else {
            log.debug("missing_instrument_repeat service=finance-api symbol={} eventId={}", event.instrumentSymbol(), event.eventId());
        }

        emitMissingInstrumentAlert(event.instrumentSymbol(), event.eventId(), "finance_kafka_missing_instrument_total");

        processedEventRepository.save(new ProcessedEvent(event.eventId()));
    }

    private void emitMissingInstrumentAlert(String symbol, String eventId, String metricName) {

        Instant now = Instant.now();

        Instant threshold = now.minusSeconds(ALERT_WINDOW_SEC);

        ConcurrentLinkedDeque<Instant> events = missingInstrumentEvents.computeIfAbsent(symbol, key -> new ConcurrentLinkedDeque<>());

        events.addLast(now);

        while (!events.isEmpty() && events.peekFirst().isBefore(threshold)) {

            events.pollFirst();
        }

        if (events.size() > 10) {

            double rate = (double) events.size() / (double) ALERT_WINDOW_SEC;

            log.warn("ALERT_SIGNAL service=finance-api metric={} symbol={} reason=missing_instrument rate={} window={}s count={} latest_event_id={}", metricName, symbol, rate, ALERT_WINDOW_SEC, events.size(), eventId);
        }
    }

    private static String extractEventIdForDeser(ConsumerRecord<String, ?> record) {
        if (record.key() != null && !record.key().isBlank()) {
            return record.key().trim();
        }
        String fromHeaders = firstHeaderMatch(record.headers(), JSON_EVENT_ID);
        return fromHeaders != null ? fromHeaders : "unknown";
    }

    private static String extractSymbolForDeser(ConsumerRecord<String, ?> record) {
        return firstHeaderMatch(record.headers(), JSON_INSTRUMENT_SYMBOL);
    }

    private static String firstHeaderMatch(Headers headers, Pattern pattern) {
        for (Header h : headers) {
            if (h.value() == null) {
                continue;
            }
            String s = new String(h.value(), StandardCharsets.UTF_8);
            Matcher m = pattern.matcher(s);
            if (m.find() && m.group(1) != null) {
                return m.group(1);
            }
        }
        return null;
    }
}
