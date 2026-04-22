package com.company.finance_api.kafka.consumer;



import com.company.finance_api.domain.Instrument;

import com.company.finance_api.domain.InstrumentPrice;

import com.company.finance_api.domain.ProcessedEvent;

import com.company.finance_api.domain.enums.PriceType;

import com.company.finance_api.kafka.MarketDataTopics;

import com.company.finance_api.kafka.event.MarketPriceUpdatedEvent;

import com.company.finance_api.repository.InstrumentRepository;

import com.company.finance_api.repository.ProcessedEventRepository;

import com.company.finance_api.service.PriceService;

import io.micrometer.core.instrument.MeterRegistry;

import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.apache.kafka.common.header.Header;

import org.apache.kafka.common.header.Headers;

import org.springframework.kafka.annotation.KafkaListener;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;



import java.nio.charset.StandardCharsets;

import java.time.Instant;

import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.ConcurrentLinkedDeque;

import java.util.concurrent.ConcurrentMap;

import java.util.regex.Matcher;

import java.util.regex.Pattern;



@Slf4j

@Component

public class MarketPriceUpdatedConsumer {



    private static final int ALERT_WINDOW_SEC = 60;

    private static final Pattern JSON_EVENT_ID = Pattern.compile("\"eventId\"\\s*:\\s*\"([^\"]+)\"");

    private static final Pattern JSON_INSTRUMENT_SYMBOL =
            Pattern.compile("\"instrumentSymbol\"\\s*:\\s*\"([^\"]+)\"");



    private final InstrumentRepository instrumentRepository;

    private final PriceService priceService;

    private final ProcessedEventRepository processedEventRepository;

    private final MeterRegistry meterRegistry;

    private final Set<String> distinctMissingSymbols = ConcurrentHashMap.newKeySet();

    private final ConcurrentMap<String, ConcurrentLinkedDeque<Instant>> missingInstrumentEvents =

            new ConcurrentHashMap<>();



    public MarketPriceUpdatedConsumer(

            InstrumentRepository instrumentRepository,

            PriceService priceService,

            ProcessedEventRepository processedEventRepository,

            MeterRegistry meterRegistry

    ) {

        this.instrumentRepository = instrumentRepository;

        this.priceService = priceService;

        this.processedEventRepository = processedEventRepository;

        this.meterRegistry = meterRegistry;

        meterRegistry.gauge("finance_kafka_missing_instrument_distinct", distinctMissingSymbols, Set::size);

    }



    @KafkaListener(

            topics = MarketDataTopics.MARKET_PRICE_UPDATED,

            groupId = "finance-api-market-price-consumer",

            containerFactory = "marketPriceKafkaListenerContainerFactory"

    )

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



        Instrument instrument = instrumentRepository.findBySymbol(event.instrumentSymbol()).orElse(null);

        if (instrument == null) {

            handleMissingInstrument(event);

            return;

        }



        PriceType priceType = PriceType.valueOf(event.priceType());



        InstrumentPrice price = InstrumentPrice.builder()

                .instrument(instrument)

                .priceType(priceType)

                .price(event.price())

                .timestamp(event.occurredAt())

                .build();



        priceService.savePrice(price);



        processedEventRepository.save(new ProcessedEvent(event.eventId()));

    }



    private void onDeserializationFailed(ConsumerRecord<String, MarketPriceUpdatedEvent> record) {

        String eventId = extractEventIdForDeser(record);

        String symbol = extractSymbolForDeser(record);

        if (symbol == null) {

            symbol = "unknown";

        }

        meterRegistry.counter(

                "finance_kafka_market_event_deserialize_errors_total",

                "service", "finance-api",

                "symbol", symbol,

                "reason", "deserialization_error"

        ).increment();

        log.warn("malformed_market_event service=finance-api topic={} partition={} offset={} eventId={} reason=deserialization_error",

                record.topic(), record.partition(), record.offset(), eventId);

    }



    private void handleMissingInstrument(MarketPriceUpdatedEvent event) {

        meterRegistry.counter(

                "finance_kafka_missing_instrument_total",

                "service", "finance-api",

                "symbol", event.instrumentSymbol(),

                "reason", "missing_instrument"

        ).increment();

        if (distinctMissingSymbols.add(event.instrumentSymbol())) {

            log.warn("missing_instrument_first_seen service=finance-api symbol={} eventId={}",

                    event.instrumentSymbol(), event.eventId());

        } else {

            log.debug("missing_instrument_repeat service=finance-api symbol={} eventId={}",

                    event.instrumentSymbol(), event.eventId());

        }

        emitMissingInstrumentAlert(event.instrumentSymbol(), event.eventId(), "finance_kafka_missing_instrument_total");

        processedEventRepository.save(new ProcessedEvent(event.eventId()));

    }



    private void emitMissingInstrumentAlert(String symbol, String eventId, String metricName) {

        Instant now = Instant.now();

        Instant threshold = now.minusSeconds(ALERT_WINDOW_SEC);

        ConcurrentLinkedDeque<Instant> events =

                missingInstrumentEvents.computeIfAbsent(symbol, key -> new ConcurrentLinkedDeque<>());

        events.addLast(now);

        while (!events.isEmpty() && events.peekFirst().isBefore(threshold)) {

            events.pollFirst();

        }

        if (events.size() > 10) {

            double rate = (double) events.size() / (double) ALERT_WINDOW_SEC;

            log.warn("ALERT_SIGNAL service=finance-api metric={} symbol={} reason=missing_instrument rate={} window={}s count={} latest_event_id={}",

                    metricName, symbol, rate, ALERT_WINDOW_SEC, events.size(), eventId);

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


