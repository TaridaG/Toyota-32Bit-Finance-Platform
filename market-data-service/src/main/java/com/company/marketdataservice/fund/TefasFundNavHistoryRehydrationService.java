package com.company.marketdataservice.fund;

import com.company.marketdataservice.config.FundMarketProperties;
import com.company.marketdataservice.event.FundSnapshotUpdatedEvent;
import com.company.marketdataservice.history.FundNavHistoryEntry;
import com.company.marketdataservice.history.FundNavHistoryRepository;
import com.company.marketdataservice.instrument.InstrumentMappingService;
import com.company.marketdataservice.service.historical.BackfillExecutionContext;
import com.company.marketdataservice.service.history.FundHistoryWriteService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists full TEFAS NAV history windows into {@code mds_fund_nav_history} without requiring the global
 * ingestion orchestrator (uses {@link BackfillExecutionContext} so Kafka publish can be skipped when configured).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TefasFundNavHistoryRehydrationService {

    private static final String PROVIDER = "TEFAS";
    private static final ZoneId TURKEY = ZoneId.of("Europe/Istanbul");
    /**
     * If the two newest NAV rows are farther apart than this (Turkey calendar days), pull TEFAS for the interior
     * window. Forward-only gap repair misses this when a fresh "today" row exists but history stopped months earlier.
     */
    private static final int INTERIOR_CALENDAR_GAP_THRESHOLD_DAYS = 10;
    private static final int INTERIOR_GAP_MAX_PASSES = 24;

    private final FundMarketProperties fundMarketProperties;
    private final TefasBindHistoryClient bindHistoryClient;
    private final InstrumentMappingService instrumentMappingService;
    private final FundHistoryWriteService fundHistoryWriteService;
    private final FundNavHistoryRepository fundNavHistoryRepository;

    public void rehydrateTrackedFundsLastYear() {
        List<String> codes = fundMarketProperties.getTrackedFundCodes();
        if (codes == null || codes.isEmpty()) {
            log.info("TEFAS_NAV_REHYDRATE_SKIP reason=no_tracked_fund_codes");
            return;
        }
        int lookback = Math.max(30, fundMarketProperties.getNavHistoryBootstrap().getLookbackDays());
        LocalDate end = bindHistoryClient.todayTurkey();
        LocalDate start = end.minusDays(lookback);
        log.info(
                "TEFAS_NAV_REHYDRATE_START funds={} window={}..{} lookbackDays={}",
                codes.size(),
                start,
                end,
                lookback
        );
        BackfillExecutionContext.activate();
        try {
            for (String raw : codes) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String code = raw.trim().toUpperCase(Locale.ROOT);
                try {
                    rehydrateOne(code, start, end);
                    Thread.sleep(800L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("TEFAS_NAV_REHYDRATE_INTERRUPTED fundCode={}", code);
                    return;
                } catch (Exception ex) {
                    log.warn("TEFAS_NAV_REHYDRATE_FAILED fundCode={} reason={}", code, ex.toString());
                }
            }
        } finally {
            BackfillExecutionContext.clear();
        }
        log.info("TEFAS_NAV_REHYDRATE_DONE");
    }

    /**
     * Fills {@code mds_fund_nav_history} from the calendar day after the latest stored NAV through today (Turkey).
     * One-shot bootstrap can stop mid-range (TEFAS chunk errors, rate limits, TP2 added after first bootstrap); this
     * repairs forward gaps without re-downloading the full lookback window.
     */
    public void repairTrackedFundNavGaps() {
        List<String> codes = fundMarketProperties.getTrackedFundCodes();
        if (codes == null || codes.isEmpty()) {
            log.info("TEFAS_NAV_GAP_REPAIR_SKIP reason=no_tracked_fund_codes");
            return;
        }
        LocalDate today = bindHistoryClient.todayTurkey();
        int lookback = Math.max(30, fundMarketProperties.getNavHistoryBootstrap().getLookbackDays());
        BackfillExecutionContext.activate();
        try {
            for (String raw : codes) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String code = raw.trim().toUpperCase(Locale.ROOT);
                try {
                    repairLargestRecentCalendarGaps(code);
                    LocalDate from = gapRepairStartDate(code, today, lookback);
                    if (from.isAfter(today)) {
                        continue;
                    }
                    log.info("TEFAS_NAV_GAP_REPAIR fundCode={} window={}..{}", code, from, today);
                    rehydrateOne(code, from, today);
                    Thread.sleep(800L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    log.warn("TEFAS_NAV_GAP_REPAIR_INTERRUPTED fundCode={}", code);
                    return;
                } catch (Exception ex) {
                    log.warn("TEFAS_NAV_GAP_REPAIR_FAILED fundCode={} reason={}", code, ex.toString());
                }
            }
        } finally {
            BackfillExecutionContext.clear();
        }
        log.info("TEFAS_NAV_GAP_REPAIR_DONE");
    }

    /**
     * Repeatedly compares the two most recent stored NAV calendar dates (Turkey). A large jump (e.g. live scheduler
     * appended "today" while bootstrap left months empty) is filled with {@link #rehydrateOne}.
     */
    private void repairLargestRecentCalendarGaps(String fundCode) throws InterruptedException {
        for (int pass = 0; pass < INTERIOR_GAP_MAX_PASSES; pass++) {
            List<FundNavHistoryEntry> top =
                    fundNavHistoryRepository.findByFundCodeOrderByObservedAtDesc(fundCode, PageRequest.of(0, 2));
            if (top.size() < 2) {
                return;
            }
            FundNavHistoryEntry latest = top.get(0);
            FundNavHistoryEntry prev = top.get(1);
            Instant latestAt = latest.getObservedAt();
            Instant prevAt = prev.getObservedAt();
            if (latestAt == null || prevAt == null) {
                return;
            }
            LocalDate latestDay = latestAt.atZone(TURKEY).toLocalDate();
            LocalDate prevDay = prevAt.atZone(TURKEY).toLocalDate();
            long gapDays = ChronoUnit.DAYS.between(prevDay, latestDay);
            if (gapDays <= INTERIOR_CALENDAR_GAP_THRESHOLD_DAYS) {
                return;
            }
            LocalDate from = prevDay.plusDays(1);
            LocalDate to = latestDay.minusDays(1);
            if (from.isAfter(to)) {
                return;
            }
            log.info(
                    "TEFAS_NAV_INTERIOR_GAP_REPAIR fundCode={} gapDays={} window={}..{} pass={}",
                    fundCode,
                    gapDays,
                    from,
                    to,
                    pass
            );
            rehydrateOne(fundCode, from, to);
            Thread.sleep(800L);
        }
        log.warn("TEFAS_NAV_INTERIOR_GAP_REPAIR_MAX_PASSES fundCode={}", fundCode);
    }

    private LocalDate gapRepairStartDate(String fundCode, LocalDate today, int lookback) {
        Optional<FundNavHistoryEntry> latest = fundNavHistoryRepository.findTopByFundCodeOrderByObservedAtDesc(fundCode);
        if (latest.isEmpty()) {
            return today.minusDays(lookback);
        }
        Instant lastAt = latest.get().getObservedAt();
        if (lastAt == null) {
            return today.minusDays(lookback);
        }
        LocalDate lastDay = lastAt.atZone(TURKEY).toLocalDate();
        if (!lastDay.isBefore(today.minusDays(1))) {
            return today.plusDays(1);
        }
        return lastDay.plusDays(1);
    }

    private void rehydrateOne(String fundCode, LocalDate start, LocalDate end) {
        JsonNode merged = bindHistoryClient.fetchMergedBindHistory(fundCode, start, end);
        List<TefasBindHistoryParsing.ParsedLatest> points =
                TefasBindHistoryParsing.allNavPointsSorted(merged, fundCode);
        if (points.isEmpty()) {
            log.warn("TEFAS_NAV_REHYDRATE_EMPTY fundCode={}", fundCode);
            return;
        }
        Long instrumentId = instrumentMappingService.resolveInstrument(PROVIDER, fundCode).orElse(null);
        List<FundSnapshotUpdatedEvent> events = new ArrayList<>(points.size());
        for (TefasBindHistoryParsing.ParsedLatest p : points) {
            if (p.nav() == null || p.nav().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            events.add(
                    new FundSnapshotUpdatedEvent(
                            stableBootstrapEventId(p.fundCode(), p.timestamp()),
                            p.fundCode(),
                            instrumentId,
                            p.nav(),
                            p.timestamp(),
                            PROVIDER
                    )
            );
        }
        if (events.isEmpty()) {
            log.warn("TEFAS_NAV_REHYDRATE_NO_VALID_NAV fundCode={}", fundCode);
            return;
        }
        fundHistoryWriteService.saveBatch(events);
        log.info("TEFAS_NAV_REHYDRATE_FUND_DONE fundCode={} rows={}", fundCode, events.size());
    }

    private static String stableBootstrapEventId(String fundCode, Instant occurredAt) {
        String raw = "TEFAS_NAV_BOOTSTRAP|"
                + (fundCode == null ? "" : fundCode.trim().toUpperCase(Locale.ROOT))
                + "|"
                + (occurredAt == null ? "" : occurredAt.toString());
        return UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
