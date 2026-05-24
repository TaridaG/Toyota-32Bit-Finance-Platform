package com.company.marketdataservice.history.infrastructure.orchestration;
import com.company.marketdataservice.history.infrastructure.http.dto.IngestionStateItemDto;
import com.company.marketdataservice.history.infrastructure.http.dto.IngestionStatusResponseDto;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillChunkEntry;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillChunkRepository;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillStateEntry;
import com.company.marketdataservice.history.infrastructure.persistence.BackfillStateRepository;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * `geçmiş veri ve backfill` infrastructure katmanı adaptörü.
 */
@Service
public class IngestionStatusQueryService {

    private final BackfillStateRepository backfillStateRepository;
    private final BackfillChunkRepository backfillChunkRepository;

    public IngestionStatusQueryService(
            BackfillStateRepository backfillStateRepository,
            BackfillChunkRepository backfillChunkRepository
    ) {
        this.backfillStateRepository = backfillStateRepository;
        this.backfillChunkRepository = backfillChunkRepository;
    }

    /**
     * Veriyi okur ve döner.
         * @param assetType girdi parametresi
         * @param status girdi parametresi
         * @param symbolPrefix girdi parametresi
         * @return işlem sonucu
         */
    public IngestionStatusResponseDto getStatus(String assetType, String status, String symbolPrefix) {
        Stream<BackfillStateEntry> stream = backfillStateRepository.findAll().stream();
        if (assetType != null && !assetType.isBlank()) {
            String normalized = assetType.trim().toUpperCase(Locale.ROOT);
            stream = stream.filter(row -> normalized.equalsIgnoreCase(row.getAssetType()));
        }
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            stream = stream.filter(row -> normalized.equalsIgnoreCase(row.getStatus()));
        }
        if (symbolPrefix != null && !symbolPrefix.isBlank()) {
            String normalized = symbolPrefix.trim().toUpperCase(Locale.ROOT);
            stream = stream.filter(row -> {
                String symbol = row.getSymbol() == null ? "" : row.getSymbol().toUpperCase(Locale.ROOT);
                return symbol.startsWith(normalized);
            });
        }

        List<BackfillStateEntry> filtered = stream
                .sorted(Comparator.comparing(BackfillStateEntry::getAssetType).thenComparing(BackfillStateEntry::getSymbol))
                .toList();

        Map<String, Long> summary = filtered.stream()
                .collect(Collectors.groupingBy(
                        row -> row.getStatus() == null ? "NOT_STARTED" : row.getStatus(),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        List<IngestionStateItemDto> items = filtered.stream()
                .map(this::toItem)
                .toList();

        return new IngestionStatusResponseDto(Instant.now(), summary, items);
    }

    private IngestionStateItemDto toItem(BackfillStateEntry row) {
        long completed = countByStatuses(row.getAssetType(), row.getSymbol(), Set.of("COMPLETED"));
        long remaining = countByStatuses(row.getAssetType(), row.getSymbol(), Set.of("PENDING", "RETRYABLE"));
        long total = completed + remaining;
        Double progress = total == 0 ? null : (double) completed / (double) total;
        BackfillChunkEntry lastChunk = backfillChunkRepository
                .findTop1ByAssetTypeAndSymbolOrderByUpdatedAtDesc(row.getAssetType(), row.getSymbol())
                .stream()
                .findFirst()
                .orElse(null);
        String lastChunkWindow = null;
        if (lastChunk != null && lastChunk.getWindowStart() != null && lastChunk.getWindowEnd() != null) {
            lastChunkWindow = lastChunk.getWindowStart() + " / " + lastChunk.getWindowEnd();
        }
        Long etaSeconds = estimateEtaSeconds(row.getAssetType(), row.getSymbol(), completed, remaining);
        return new IngestionStateItemDto(
                row.getAssetType(),
                row.getSymbol(),
                row.getStatus(),
                row.getLastFetchedAt(),
                progress,
                completed,
                remaining,
                etaSeconds,
                lastChunkWindow,
                row.getLockedBy(),
                row.getAttemptCount(),
                row.getNextRetryAt(),
                row.getErrorCode(),
                row.getErrorMessage(),
                row.getRunId(),
                row.getUpdatedAt()
        );
    }

    private long countByStatuses(String assetType, String symbol, Collection<String> statuses) {
        return backfillChunkRepository.countByAssetTypeAndSymbolAndStatusIn(assetType, symbol, statuses);
    }

    private Long estimateEtaSeconds(String assetType, String symbol, long completed, long remaining) {
        if (completed <= 0 || remaining <= 0) {
            return null;
        }
        Object[] bounds = backfillChunkRepository.getCompletedWindowBounds(assetType, symbol);
        if (bounds == null || bounds.length < 2 || bounds[0] == null || bounds[1] == null) {
            return null;
        }
        Instant started = toInstant(bounds[0]);
        Instant ended = toInstant(bounds[1]);
        if (started == null || ended == null) {
            return null;
        }
        long elapsed = Math.max(1L, ended.getEpochSecond() - started.getEpochSecond());
        double chunksPerSecond = (double) completed / (double) elapsed;
        if (chunksPerSecond <= 0d) {
            return null;
        }
        return Math.max(1L, Math.round(remaining / chunksPerSecond));
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Instant i) {
            return i;
        }
        if (value instanceof OffsetDateTime odt) {
            return odt.toInstant();
        }
        if (value instanceof Timestamp ts) {
            return ts.toInstant();
        }
        return null;
    }
}
