package com.company.marketdataservice.history.infrastructure.write;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryEntry;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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
    private final MarketPriceHistoryRepository repository;

    /**
     * Veriyi persist eder.
         * @param event girdi parametresi
         */
    public void save(MarketPriceUpdatedEvent event) {
        MarketPriceHistoryEntry entry = toEntry(event);
        if (entry == null) {
            return;
        }
        try {
            repository.save(entry);
            log.info(
                    "DB WRITE symbol={} price={} time={}",
                    entry.getInstrumentSymbol(),
                    entry.getPrice(),
                    entry.getObservedAt()
            );
        } catch (DataIntegrityViolationException ex) {
            log.debug(
                    "market_history_duplicate_ignored eventId={} symbol={} provider={} observedAt={} reason={}",
                    event.eventId(),
                    event.instrumentSymbol(),
                    event.source(),
                    event.occurredAt(),
                    ex.getClass().getSimpleName()
            );
        }
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
        List<MarketPriceUpdatedEvent> validEvents = new ArrayList<>();
        for (MarketPriceUpdatedEvent event : events) {
            MarketPriceHistoryEntry entry = toEntry(event);
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
            List<MarketPriceHistoryEntry> batch = entries.subList(i, end);
            try {
                repository.saveAll(batch);
                for (MarketPriceHistoryEntry entry : batch) {
                    log.info(
                            "DB WRITE symbol={} price={} time={}",
                            entry.getInstrumentSymbol(),
                            entry.getPrice(),
                            entry.getObservedAt()
                    );
                }
            } catch (DataIntegrityViolationException ex) {
                for (MarketPriceHistoryEntry entry : batch) {
                    try {
                        repository.save(entry);
                    } catch (DataIntegrityViolationException duplicateEx) {
                        log.debug(
                                "market_history_duplicate_ignored eventId={} symbol={} provider={} observedAt={} reason={}",
                                entry.getEventId(),
                                entry.getInstrumentSymbol(),
                                entry.getProvider(),
                                entry.getObservedAt(),
                                duplicateEx.getClass().getSimpleName()
                        );
                    }
                }
            }
        }
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
