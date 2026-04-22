package com.company.analytics.infrastructure.kafka;

import com.company.analytics.application.CandleAggregationService;
import com.company.analytics.application.EventIdempotencyService;
import com.company.analytics.application.MovingAverageService;
import com.company.analytics.application.RSIService;
import com.company.analytics.application.TrendMetricService;
import com.company.analytics.client.FinanceInstrumentClient;
import com.company.analytics.event.AnalyticsMarketPriceEvent;
import com.company.analytics.event.MarketPriceUpdatedEvent;
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

    private final CandleAggregationService candleAggregationService;
    private final EventIdempotencyService eventIdempotencyService;
    private final MovingAverageService movingAverageService;
    private final RSIService rsiService;
    private final TrendMetricService trendMetricService;
    private final FinanceInstrumentClient financeInstrumentClient;
    private final MeterRegistry meterRegistry;
    private final Set<String> distinctMissingSymbols = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, ConcurrentLinkedDeque<Instant>> missingInstrumentEvents =
            new ConcurrentHashMap<>();

    public MarketPriceUpdatedConsumer(
            CandleAggregationService candleAggregationService,
            EventIdempotencyService eventIdempotencyService,
            MovingAverageService movingAverageService,
            RSIService rsiService,
            TrendMetricService trendMetricService,
            FinanceInstrumentClient financeInstrumentClient,
            MeterRegistry meterRegistry
    ) {
        this.candleAggregationService = candleAggregationService;
        this.eventIdempotencyService = eventIdempotencyService;
        this.movingAverageService = movingAverageService;
        this.rsiService = rsiService;
        this.trendMetricService = trendMetricService;
        this.financeInstrumentClient = financeInstrumentClient;
        this.meterRegistry = meterRegistry;
        meterRegistry.gauge("analytics_kafka_missing_instrument_distinct", distinctMissingSymbols, Set::size);
    }

    @Transactional
    @KafkaListener(
            topics = "market.price.updated",
            groupId = "analytics-service",
            containerFactory = "analyticsKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, MarketPriceUpdatedEvent> record) {
        if (record.value() == null) {
            onDeserializationFailed(record);
            return;
        }
        MarketPriceUpdatedEvent m = record.value();
        if (eventIdempotencyService.isProcessed(m.eventId())) {
            log.info("Duplicate market event skipped: {}", m.eventId());
            return;
        }

        Long instrumentId = financeInstrumentClient.resolveInstrumentId(m.instrumentSymbol()).orElse(null);
        if (instrumentId == null) {
            handleMissingInstrument(m);
            return;
        }

        AnalyticsMarketPriceEvent event = new AnalyticsMarketPriceEvent(
                m.eventId(),
                instrumentId,
                m.instrumentSymbol(),
                m.price(),
                m.occurredAt()
        );

        candleAggregationService.process(event);
        movingAverageService.process(event);
        rsiService.process(event);
        trendMetricService.process(event);
        eventIdempotencyService.markProcessed(event.eventId());

        log.info("Analytics processed market event for symbol={} eventId={}",
                event.instrumentSymbol(), event.eventId());
    }

    private void onDeserializationFailed(ConsumerRecord<String, MarketPriceUpdatedEvent> record) {
        String eventId = extractEventIdForDeser(record);
        String symbol = extractSymbolForDeser(record);
        if (symbol == null) {
            symbol = "unknown";
        }
        meterRegistry.counter(
                "analytics_kafka_market_event_deserialize_errors_total",
                "service", "analytics-service",
                "symbol", symbol,
                "reason", "deserialization_error"
        ).increment();
        log.warn("malformed_market_event service=analytics-service topic={} partition={} offset={} eventId={} reason=deserialization_error",
                record.topic(), record.partition(), record.offset(), eventId);
    }

    private void handleMissingInstrument(MarketPriceUpdatedEvent m) {
        meterRegistry.counter(
                "analytics_kafka_missing_instrument_total",
                "service", "analytics-service",
                "symbol", m.instrumentSymbol(),
                "reason", "missing_instrument"
        ).increment();
        if (distinctMissingSymbols.add(m.instrumentSymbol())) {
            log.warn("missing_instrument_first_seen service=analytics-service symbol={} eventId={}",
                    m.instrumentSymbol(), m.eventId());
        } else {
            log.debug("missing_instrument_repeat service=analytics-service symbol={} eventId={}",
                    m.instrumentSymbol(), m.eventId());
        }
        emitMissingInstrumentAlert(m.instrumentSymbol(), m.eventId(), "analytics_kafka_missing_instrument_total");
        eventIdempotencyService.markProcessed(m.eventId());
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
            log.warn("ALERT_SIGNAL service=analytics-service metric={} symbol={} reason=missing_instrument rate={} window={}s count={} latest_event_id={}",
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
