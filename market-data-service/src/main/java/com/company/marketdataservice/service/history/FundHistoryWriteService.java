package com.company.marketdataservice.service.history;

import com.company.marketdataservice.event.FundSnapshotUpdatedEvent;
import com.company.marketdataservice.history.FundNavHistoryEntry;
import com.company.marketdataservice.history.FundNavHistoryRepository;
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
public class FundHistoryWriteService {

    private static final int BATCH_SIZE = 250;
    private final FundNavHistoryRepository repository;

    @Transactional
    public void save(FundSnapshotUpdatedEvent event) {
        FundNavHistoryEntry entry = toEntry(event);
        if (entry == null) {
            return;
        }
        try {
            repository.save(entry);
        } catch (DataIntegrityViolationException ex) {
            log.debug(
                    "fund_history_duplicate_ignored eventId={} fundCode={} provider={} observedAt={} reason={}",
                    event.eventId(),
                    event.fundCode(),
                    event.source(),
                    event.occurredAt(),
                    ex.getClass().getSimpleName()
            );
        }
    }

    @Transactional
    public void saveBatch(List<FundSnapshotUpdatedEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        List<FundNavHistoryEntry> entries = new ArrayList<>();
        List<FundSnapshotUpdatedEvent> validEvents = new ArrayList<>();
        for (FundSnapshotUpdatedEvent event : events) {
            FundNavHistoryEntry entry = toEntry(event);
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
                for (FundSnapshotUpdatedEvent event : validEvents.subList(i, end)) {
                    save(event);
                }
            }
        }
    }

    private static FundNavHistoryEntry toEntry(FundSnapshotUpdatedEvent event) {
        if (event == null || event.fundCode() == null || event.fundCode().isBlank() || event.nav() == null) {
            return null;
        }
        FundNavHistoryEntry entry = new FundNavHistoryEntry();
        entry.setInstrumentId(event.instrumentId());
        entry.setFundCode(event.fundCode().trim().toUpperCase(Locale.ROOT));
        entry.setNav(event.nav());
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
