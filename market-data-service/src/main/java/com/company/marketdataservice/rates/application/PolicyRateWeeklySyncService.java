package com.company.marketdataservice.rates.application;
import com.company.marketdataservice.rates.domain.PolicyRateWeeklyAggregator;
import com.company.marketdataservice.rates.infrastructure.persistence.TcmbPolicyRateWeeklyEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.TcmbPolicyRateWeeklyRepository;
import com.company.marketdataservice.bootstrap.config.MarketEvdsProperties;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * `makro oran` application katmanı use-case servisi.
 */
@Service
public class PolicyRateWeeklySyncService {

    private static final Logger log = LoggerFactory.getLogger(PolicyRateWeeklySyncService.class);
    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");

    private final TcmbPolicyRateWeeklyRepository weeklyRepository;
    private final TcmbBondEvdsClient tcmbBondEvdsClient;
    private final MarketEvdsProperties evdsProperties;
    private final AtomicBoolean asyncKickoffInFlight = new AtomicBoolean(false);

    public PolicyRateWeeklySyncService(
            TcmbPolicyRateWeeklyRepository weeklyRepository,
            TcmbBondEvdsClient tcmbBondEvdsClient,
            MarketEvdsProperties evdsProperties
    ) {
        this.weeklyRepository = weeklyRepository;
        this.tcmbBondEvdsClient = tcmbBondEvdsClient;
        this.evdsProperties = evdsProperties;
    }

    /**
     * If the DB has no rows yet, kick a virtual-thread sync once (non-blocking for HTTP threads).
     */
    public void requestInitialSyncIfEmptyAsync() {
        if (weeklyRepository.count() > 0) {
            return;
        }
        if (!asyncKickoffInFlight.compareAndSet(false, true)) {
            return;
        }
        Thread.ofVirtual()
                .name("mds-policy-rate-weekly-initial")
                .start(() -> {
                    try {
                        syncIfNeeded();
                    } catch (Exception ex) {
                        log.warn("POLICY_RATE_WEEKLY_INITIAL_SYNC_FAILED reason={}", ex.toString());
                    } finally {
                        asyncKickoffInFlight.set(false);
                    }
                });
    }

    /**
     * Refreshes the trailing 5-year ISO-week table from EVDS on every run (bootstrap + weekly cron).
     * <p>Previously we only fetched when a Monday key was missing; that left bad rows (wrong EVDS column / locale)
     * in the DB forever after a one-time bad ingest.</p>
     */
    public void syncIfNeeded() {
        if (!StringUtils.hasText(evdsProperties.getApiKey())) {
            log.warn("POLICY_RATE_WEEKLY_SYNC_SKIP no EVDS apiKey");
            return;
        }
        String series = evdsProperties.getPolicyRateSeries();
        if (!StringUtils.hasText(series)) {
            log.warn("POLICY_RATE_WEEKLY_SYNC_SKIP blank policyRateSeries");
            return;
        }

        LocalDate rangeEnd = LocalDate.now(TR);
        LocalDate rangeStart = rangeEnd.minusYears(5).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        log.info("POLICY_RATE_WEEKLY_SYNC_START window={}..{} series={}", rangeStart, rangeEnd, series.trim());

        LocalDate fetchStart = rangeStart.minusYears(3);
        List<BondEodPoint> daily;
        try {
            String freq = StringUtils.hasText(evdsProperties.getPolicyRateEvdsFrequency())
                    ? evdsProperties.getPolicyRateEvdsFrequency().trim()
                    : null;
            daily = tcmbBondEvdsClient.fetchHistoryPoints(series.trim(), fetchStart, rangeEnd, freq);
        } catch (Exception ex) {
            log.warn("POLICY_RATE_WEEKLY_SYNC_EVDS_FAILED reason={}", ex.toString(), ex);
            return;
        }
        daily.sort(java.util.Comparator.comparing(BondEodPoint::date));
        Map<LocalDate, PolicyRateWeeklyAggregator.WeekPolicyPick> byWeek =
                PolicyRateWeeklyAggregator.lastPickByWeekMonday(daily, rangeStart, rangeEnd);

        Instant now = Instant.now();
        for (Map.Entry<LocalDate, PolicyRateWeeklyAggregator.WeekPolicyPick> e : byWeek.entrySet()) {
            TcmbPolicyRateWeeklyEntity row = weeklyRepository.findByWeekStart(e.getKey()).orElseGet(TcmbPolicyRateWeeklyEntity::new);
            row.setWeekStart(e.getKey());
            PolicyRateWeeklyAggregator.WeekPolicyPick pick = e.getValue();
            row.setRatePercent(pick.ratePercent());
            row.setSourceObservationDate(pick.evdsObservationDate());
            row.setSourceProvider("TCMB_EVDS");
            row.setIngestTime(now);
            weeklyRepository.save(row);
        }
        log.info("POLICY_RATE_WEEKLY_SYNC_DONE upsertedWeeks={}", byWeek.size());
    }
}
