package com.company.marketdataservice.history.infrastructure.write;
import com.company.marketdataservice.fx.domain.FxSnapshotUpdatedEvent;
import com.company.marketdataservice.history.infrastructure.persistence.FxRateHistoryEntry;
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
public class FxHistoryWriteService {

    private static final int BATCH_SIZE = 250;
    private static final String INSERT_IGNORE_DUPLICATE = """
            INSERT INTO mds_fx_rate_history
                (instrument_id, canonical_symbol, base_currency, quote_currency, bid, ask, mid, provider, observed_at, event_id, ingest_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (canonical_symbol, provider, observed_at) DO NOTHING
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * Veriyi persist eder.
         * @param event girdi parametresi
         */
    public void save(FxSnapshotUpdatedEvent event) {
        FxRateHistoryEntry entry = toEntry(event);
        if (entry == null) {
            return;
        }
        persistEntries(List.of(entry));
    }

    /**
     * Veriyi persist eder.
         * @param events girdi parametresi
         */
    public void saveBatch(List<FxSnapshotUpdatedEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        List<FxRateHistoryEntry> entries = new ArrayList<>();
        for (FxSnapshotUpdatedEvent event : events) {
            FxRateHistoryEntry entry = toEntry(event);
            if (entry != null) {
                entries.add(entry);
            }
        }
        if (entries.isEmpty()) {
            return;
        }
        persistEntries(entries);
    }

    private void persistEntries(List<FxRateHistoryEntry> entries) {
        for (int i = 0; i < entries.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, entries.size());
            List<FxRateHistoryEntry> batch = entries.subList(i, end);
            jdbcTemplate.batchUpdate(INSERT_IGNORE_DUPLICATE, batch, batch.size(), (ps, entry) -> {
                if (entry.getInstrumentId() != null) {
                    ps.setLong(1, entry.getInstrumentId());
                } else {
                    ps.setObject(1, null);
                }
                ps.setString(2, entry.getCanonicalSymbol());
                ps.setString(3, entry.getBaseCurrency());
                ps.setString(4, entry.getQuoteCurrency());
                ps.setBigDecimal(5, entry.getBid());
                ps.setBigDecimal(6, entry.getAsk());
                ps.setBigDecimal(7, entry.getMid());
                ps.setString(8, entry.getProvider());
                ps.setTimestamp(9, Timestamp.from(entry.getObservedAt()));
                ps.setObject(10, entry.getEventId());
                ps.setTimestamp(11, Timestamp.from(entry.getIngestTime()));
            });
        }
        log.debug("fx_history_persisted rows={}", entries.size());
    }

    private static FxRateHistoryEntry toEntry(FxSnapshotUpdatedEvent event) {
        if (event == null || event.canonicalSymbol() == null || event.canonicalSymbol().isBlank() || event.mid() == null) {
            return null;
        }
        FxRateHistoryEntry entry = new FxRateHistoryEntry();
        entry.setInstrumentId(event.instrumentId());
        entry.setCanonicalSymbol(event.canonicalSymbol().trim().toUpperCase(Locale.ROOT));
        entry.setBaseCurrency(normalizeOrUnknown(event.baseCurrency()));
        entry.setQuoteCurrency(normalizeOrUnknown(event.quoteCurrency()));
        entry.setBid(event.bid());
        entry.setAsk(event.ask());
        entry.setMid(event.mid());
        entry.setProvider(normalizeOrUnknown(event.source()));
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
