package com.company.marketdataservice.service.history;

import com.company.marketdataservice.event.FxSnapshotUpdatedEvent;
import com.company.marketdataservice.history.FxRateHistoryEntry;
import com.company.marketdataservice.history.FxRateHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FxHistoryWriteService {

    private static final int BATCH_SIZE = 250;
    private final FxRateHistoryRepository repository;

    @Transactional
    public void save(FxSnapshotUpdatedEvent event) {
        FxRateHistoryEntry entry = toEntry(event);
        if (entry == null) {
            return;
        }
        try {
            repository.save(entry);
        } catch (DataIntegrityViolationException ex) {
            log.debug(
                    "fx_history_duplicate_ignored eventId={} symbol={} provider={} observedAt={} reason={}",
                    event.eventId(),
                    event.canonicalSymbol(),
                    event.source(),
                    event.occurredAt(),
                    ex.getClass().getSimpleName()
            );
        }
    }

    @Transactional
    public void saveBatch(List<FxSnapshotUpdatedEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        List<FxRateHistoryEntry> entries = new ArrayList<>();
        List<FxSnapshotUpdatedEvent> validEvents = new ArrayList<>();
        for (FxSnapshotUpdatedEvent event : events) {
            FxRateHistoryEntry entry = toEntry(event);
            if (entry != null) {
                entries.add(entry);
                validEvents.add(event);
            }
        }
        if (entries.isEmpty()) {
            return;
        }
        for (int i = 0; i < entries.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, entries.size());
            try {
                repository.saveAll(entries.subList(i, end));
            } catch (DataIntegrityViolationException ex) {
                for (FxSnapshotUpdatedEvent event : validEvents.subList(i, end)) {
                    save(event);
                }
            }
        }
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
