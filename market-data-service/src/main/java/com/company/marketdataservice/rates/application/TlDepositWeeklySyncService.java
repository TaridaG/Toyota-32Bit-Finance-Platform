package com.company.marketdataservice.rates.application;
import com.company.marketdataservice.rates.domain.PolicyRateWeeklyAggregator;
import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositWeeklyEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositWeeklyRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * `makro oran` application katmanı use-case servisi.
 */
@Service
public class TlDepositWeeklySyncService {

    private static final Logger log = LoggerFactory.getLogger(TlDepositWeeklySyncService.class);
    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");

    private final TlDepositWeeklyRepository tlDepositWeeklyRepository;
    private final TcmbBondEvdsClient tcmbBondEvdsClient;
    private final MarketEvdsProperties evdsProperties;
    private final TlDepositDailyIndexSyncService tlDepositDailyIndexSyncService;
    private final AtomicBoolean asyncKickoffInFlight = new AtomicBoolean(false);

    public TlDepositWeeklySyncService(
            TlDepositWeeklyRepository tlDepositWeeklyRepository,
            TcmbBondEvdsClient tcmbBondEvdsClient,
            MarketEvdsProperties evdsProperties,
            TlDepositDailyIndexSyncService tlDepositDailyIndexSyncService
    ) {
        this.tlDepositWeeklyRepository = tlDepositWeeklyRepository;
        this.tcmbBondEvdsClient = tcmbBondEvdsClient;
        this.evdsProperties = evdsProperties;
        this.tlDepositDailyIndexSyncService = tlDepositDailyIndexSyncService;
    }

    /**
     * If the display-maturity slice has no rows yet, kick a virtual-thread sync once (non-blocking for HTTP threads).
     */
    public void requestInitialSyncIfEmptyAsync() {
        String display = displayMaturityCode();
        if (tlDepositWeeklyRepository.countByMaturityCode(display) > 0) {
            return;
        }
        if (!asyncKickoffInFlight.compareAndSet(false, true)) {
            return;
        }
        Thread.ofVirtual()
                .name("mds-tl-deposit-weekly-initial")
                .start(() -> {
                    try {
                        syncIfNeeded();
                    } catch (Exception ex) {
                        log.warn("TL_DEPOSIT_WEEKLY_INITIAL_SYNC_FAILED reason={}", ex.toString());
                    } finally {
                        asyncKickoffInFlight.set(false);
                    }
                });
    }

    /**
     * Refreshes trailing-window weekly rows for each configured EVDS series (bootstrap + weekly cron).
     */
    public void syncIfNeeded() {
        if (!StringUtils.hasText(evdsProperties.getApiKey())) {
            log.warn("TL_DEPOSIT_WEEKLY_SYNC_SKIP no EVDS apiKey");
            return;
        }
        List<String> seriesList = evdsProperties.getTlDepositEvdsSeries();
        if (seriesList == null || seriesList.isEmpty()) {
            log.warn("TL_DEPOSIT_WEEKLY_SYNC_SKIP empty tlDepositEvdsSeries");
            return;
        }

        LocalDate rangeEnd = LocalDate.now(TR);
        LocalDate rangeStart = rangeEnd.minusYears(5).with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
        LocalDate fetchStart = rangeStart.minusYears(3);
        String freq = StringUtils.hasText(evdsProperties.getTlDepositEvdsFrequency())
                ? evdsProperties.getTlDepositEvdsFrequency().trim()
                : null;

        Instant now = Instant.now();
        int totalWeeks = 0;
        for (String rawSeries : seriesList) {
            if (!StringUtils.hasText(rawSeries)) {
                continue;
            }
            String series = rawSeries.trim();
            String maturity = maturitySuffixFromSeries(series);
            log.info("TL_DEPOSIT_WEEKLY_SYNC_START window={}..{} series={} maturity={}", rangeStart, rangeEnd, series, maturity);
            List<BondEodPoint> daily;
            try {
                daily = tcmbBondEvdsClient.fetchHistoryPoints(series, fetchStart, rangeEnd, freq);
            } catch (Exception ex) {
                log.warn("TL_DEPOSIT_WEEKLY_SYNC_EVDS_FAILED series={} reason={}", series, ex.toString(), ex);
                continue;
            }
            daily.sort(java.util.Comparator.comparing(BondEodPoint::date));
            Map<LocalDate, PolicyRateWeeklyAggregator.WeekPolicyPick> byWeek =
                    PolicyRateWeeklyAggregator.lastPickByWeekMonday(daily, rangeStart, rangeEnd);
            for (Map.Entry<LocalDate, PolicyRateWeeklyAggregator.WeekPolicyPick> e : byWeek.entrySet()) {
                TlDepositWeeklyEntity row = tlDepositWeeklyRepository
                        .findByWeekStartAndMaturityCode(e.getKey(), maturity)
                        .orElseGet(TlDepositWeeklyEntity::new);
                row.setWeekStart(e.getKey());
                row.setMaturityCode(maturity);
                PolicyRateWeeklyAggregator.WeekPolicyPick pick = e.getValue();
                row.setRatePercent(pick.ratePercent());
                row.setSourceObservationDate(pick.evdsObservationDate());
                row.setSourceProvider("TCMB_EVDS");
                row.setIngestTime(now);
                tlDepositWeeklyRepository.save(row);
            }
            totalWeeks += byWeek.size();
            log.info("TL_DEPOSIT_WEEKLY_SYNC_DONE series={} maturity={} upsertedWeeks={}", series, maturity, byWeek.size());
        }
        tlDepositDailyIndexSyncService.rebuildForMaturities(configuredMaturitySuffixes());
        log.info("TL_DEPOSIT_WEEKLY_SYNC_ALL_DONE totalUpsertWeeksAcrossSeries={}", totalWeeks);
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    public String displayMaturityCode() {
        String code = evdsProperties.getTlDepositCardMaturity();
        return StringUtils.hasText(code) ? code.trim().toUpperCase(Locale.ROOT) : "MT04";
    }

    /** Maturity codes (MT01…) derived from configured EVDS series list, in YAML order. */
    public Set<String> configuredMaturitySuffixes() {
        List<String> list = evdsProperties.getTlDepositEvdsSeries();
        if (list == null || list.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String s : list) {
            if (!StringUtils.hasText(s)) {
                continue;
            }
            out.add(maturitySuffixFromSeries(s.trim()));
        }
        return out;
    }

    static String maturitySuffixFromSeries(String evdsSeries) {
        String t = evdsSeries.trim().toUpperCase(Locale.ROOT);
        int dot = t.lastIndexOf('.');
        int us = t.lastIndexOf('_');
        int i = Math.max(dot, us);
        return i < 0 ? t : t.substring(i + 1);
    }
}
