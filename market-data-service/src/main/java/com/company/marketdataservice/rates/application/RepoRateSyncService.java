package com.company.marketdataservice.rates.application;

import com.company.marketdataservice.bootstrap.config.MarketEvdsProperties;
import com.company.marketdataservice.rates.infrastructure.persistence.TcmbRepoRatePointEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.TcmbRepoRatePointRepository;
import com.company.marketdataservice.shared.provider.tcmb.BondEodPoint;
import com.company.marketdataservice.shared.provider.tcmb.TcmbBondEvdsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RepoRateSyncService {

    private static final Logger log = LoggerFactory.getLogger(RepoRateSyncService.class);
    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");

    private final TcmbRepoRatePointRepository pointRepository;
    private final TcmbBondEvdsClient tcmbBondEvdsClient;
    private final MarketEvdsProperties evdsProperties;
    private final AtomicBoolean asyncKickoffInFlight = new AtomicBoolean(false);

    public RepoRateSyncService(
            TcmbRepoRatePointRepository pointRepository,
            TcmbBondEvdsClient tcmbBondEvdsClient,
            MarketEvdsProperties evdsProperties
    ) {
        this.pointRepository = pointRepository;
        this.tcmbBondEvdsClient = tcmbBondEvdsClient;
        this.evdsProperties = evdsProperties;
    }

    public void requestInitialSyncIfEmptyAsync() {
        if (pointRepository.count() > 0) {
            return;
        }
        if (!asyncKickoffInFlight.compareAndSet(false, true)) {
            return;
        }
        Thread.ofVirtual()
                .name("mds-repo-rate-initial")
                .start(() -> {
                    try {
                        syncIfNeeded();
                    } catch (Exception ex) {
                        log.warn("REPO_RATE_INITIAL_SYNC_FAILED reason={}", ex.toString());
                    } finally {
                        asyncKickoffInFlight.set(false);
                    }
                });
    }

    public void syncIfNeeded() {
        if (!StringUtils.hasText(evdsProperties.getApiKey())) {
            log.warn("REPO_RATE_SYNC_SKIP no EVDS apiKey");
            return;
        }
        String series = evdsProperties.getRepoRateSeries();
        if (!StringUtils.hasText(series)) {
            log.warn("REPO_RATE_SYNC_SKIP blank repoRateSeries");
            return;
        }

        LocalDate rangeEnd = LocalDate.now(TR);
        LocalDate rangeStart = rangeEnd.minusYears(5).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate fetchStart = rangeStart.minusYears(3);

        log.info("REPO_RATE_SYNC_START window={}..{} series={}", rangeStart, rangeEnd, series.trim());

        List<BondEodPoint> evdsPoints;
        try {
            String freq = StringUtils.hasText(evdsProperties.getRepoRateEvdsFrequency())
                    ? evdsProperties.getRepoRateEvdsFrequency().trim()
                    : null;
            evdsPoints = tcmbBondEvdsClient.fetchHistoryPoints(series.trim(), fetchStart, rangeEnd, freq);
        } catch (Exception ex) {
            log.warn("REPO_RATE_SYNC_EVDS_FAILED reason={}", ex.toString(), ex);
            return;
        }
        evdsPoints.sort(Comparator.comparing(BondEodPoint::date));
        if (evdsPoints.isEmpty()) {
            log.warn("REPO_RATE_SYNC_EMPTY series={}", series.trim());
            return;
        }

        LocalDate firstEvdsDate = evdsPoints.getFirst().date();
        LocalDate cursorStart = rangeStart.isBefore(firstEvdsDate) ? firstEvdsDate : rangeStart;

        Instant now = Instant.now();
        int upserted = 0;
        int pointIdx = 0;
        BondEodPoint current = evdsPoints.get(pointIdx);

        for (LocalDate d = cursorStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
            while (pointIdx + 1 < evdsPoints.size() && !evdsPoints.get(pointIdx + 1).date().isAfter(d)) {
                pointIdx++;
                current = evdsPoints.get(pointIdx);
            }
            if (current.date().isAfter(d)) {
                continue;
            }

            TcmbRepoRatePointEntity row = pointRepository.findByObservationDate(d).orElseGet(TcmbRepoRatePointEntity::new);
            row.setObservationDate(d);
            row.setRatePercent(current.value());
            row.setSourceProvider("TCMB_EVDS");
            row.setIngestTime(now);
            pointRepository.save(row);
            upserted++;
        }
        log.info("REPO_RATE_SYNC_DONE upsertedDays={}", upserted);
    }
}
