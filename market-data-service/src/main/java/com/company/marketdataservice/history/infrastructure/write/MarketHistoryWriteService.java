package com.company.marketdataservice.history.infrastructure.write;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * `geçmiş veri ve backfill` infrastructure katmanı adaptörü.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketHistoryWriteService {

    private static final int BATCH_SIZE = 250;
    private static final String INSERT_IGNORE_DUPLICATE = """
            INSERT INTO mds_market_price_history
                (instrument_id, instrument_symbol, provider, source_symbol, price, price_type, observed_at, event_id, ingest_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (instrument_symbol, provider, observed_at, price_type) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Veriyi persist eder.
         * @param event girdi parametresi
         */
    public void save(MarketPriceUpdatedEvent event) {
        MarketPriceHistoryEntry entry = toEntry(event);
        if (entry == null) {
            return;
        }
        persistEntries(List.of(entry));
    }

    /**
     * Veriyi persist eder.
         * @param events girdi parametresi
         */
    public void saveBatch(List<MarketPriceUpdatedEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        List<MarketPriceHistoryEntry> entries = new ArrayList<>();
        for (MarketPriceUpdatedEvent event : events) {
            MarketPriceHistoryEntry entry = toEntry(event);
            if (entry != null) {
                entries.add(entry);
            }
        }
        if (entries.isEmpty()) {
            return;
        }
        persistEntries(entries);
    }

    private void persistEntries(List<MarketPriceHistoryEntry> entries) {
        for (int i = 0; i < entries.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, entries.size());
            List<MarketPriceHistoryEntry> batch = entries.subList(i, end);
            jdbcTemplate.batchUpdate(INSERT_IGNORE_DUPLICATE, batch, batch.size(), (ps, entry) -> {
                if (entry.getInstrumentId() != null) {
                    ps.setLong(1, entry.getInstrumentId());
                } else {
                    ps.setObject(1, null);
                }
                ps.setString(2, entry.getInstrumentSymbol());
                ps.setString(3, entry.getProvider());
                ps.setString(4, entry.getSourceSymbol());
                ps.setBigDecimal(5, entry.getPrice());
                ps.setString(6, entry.getPriceType());
                ps.setTimestamp(7, Timestamp.from(entry.getObservedAt()));
                ps.setObject(8, entry.getEventId());
                ps.setTimestamp(9, Timestamp.from(entry.getIngestTime()));
            });
        }
        log.debug("market_history_persisted rows={}", entries.size());
    }

    private static MarketPriceHistoryEntry toEntry(MarketPriceUpdatedEvent event) {
        if (event == null || event.instrumentSymbol() == null || event.instrumentSymbol().isBlank() || event.price() == null) {
            return null;
        }
        MarketPriceHistoryEntry entry = new MarketPriceHistoryEntry();
        entry.setInstrumentId(event.instrumentId());
        entry.setInstrumentSymbol(event.instrumentSymbol().trim().toUpperCase(Locale.ROOT));
        entry.setProvider(normalizeOrUnknown(event.source()));
        entry.setSourceSymbol(event.instrumentSymbol().trim());
        entry.setPrice(event.price());
        entry.setPriceType(normalizeOrUnknown(event.priceType()));
        entry.setObservedAt(event.occurredAt() == null ? Instant.now() : event.occurredAt());
        entry.setEventId(parseUuid(event.eventId()));
        entry.setIngestTime(Instant.now());
        return entry;
    }

    private static String normalizeOrUnknown(String value) {
        return (value == null || value.isBlank()) ? "UNKNOWN" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static UUID parseUuid(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
