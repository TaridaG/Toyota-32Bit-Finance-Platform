package com.company.marketdataservice.history.infrastructure.write;
import com.company.marketdataservice.fund.domain.FundSnapshotUpdatedEvent;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryEntry;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
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
public class FundHistoryWriteService {

    private static final int BATCH_SIZE = 250;
    private final FundNavHistoryRepository repository;

    /**
     * Veriyi persist eder.
         * @param event girdi parametresi
         */
    @Transactional
    public void save(FundSnapshotUpdatedEvent event) {
        FundNavHistoryEntry entry = toEntry(event);
        if (entry == null) {
            return;
        }
        try {
            repository.save(entry);
        } catch (DataIntegrityViolationException ex) {
            logDuplicateIgnored(event, ex);
        } catch (DataAccessException ex) {
            if (isDuplicateKey(ex)) {
                logDuplicateIgnored(event, ex);
            } else {
                throw ex;
            }
        }
    }

    /**
     * Veriyi persist eder.
         * @param events girdi parametresi
         */
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
                saveEventsOneByOne(validEvents.subList(i, end));
            } catch (DataAccessException ex) {
                if (isDuplicateKey(ex)) {
                    log.debug(
                            "fund_history_batch_save_duplicate_fallback batchStart={} batchEnd={} reason={}",
                            i,
                            end,
                            ex.getClass().getSimpleName()
                    );
                    saveEventsOneByOne(validEvents.subList(i, end));
                } else {
                    throw ex;
                }
            }
        }
    }

    private void saveEventsOneByOne(List<FundSnapshotUpdatedEvent> slice) {
        for (FundSnapshotUpdatedEvent event : slice) {
            save(event);
        }
    }

    private void logDuplicateIgnored(FundSnapshotUpdatedEvent event, Throwable ex) {
        log.debug(
                "fund_history_duplicate_ignored eventId={} fundCode={} provider={} observedAt={} reason={}",
                event.eventId(),
                event.fundCode(),
                event.source(),
                event.occurredAt(),
                ex.getClass().getSimpleName()
        );
    }

    private static boolean isDuplicateKey(DataAccessException ex) {
        if (ex instanceof DataIntegrityViolationException) {
            return true;
        }
        for (Throwable cur = ex; cur != null; cur = cur.getCause()) {
            if (cur instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
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
