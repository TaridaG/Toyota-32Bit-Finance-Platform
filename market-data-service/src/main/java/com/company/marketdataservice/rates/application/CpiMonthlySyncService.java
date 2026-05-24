package com.company.marketdataservice.rates.application;
import com.company.marketdataservice.rates.domain.CpiMetric;
import com.company.marketdataservice.rates.infrastructure.persistence.MdsCpiMonthlyEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.MdsCpiMonthlyRepository;
import com.company.marketdataservice.bootstrap.config.MarketEvdsProperties;
import com.company.marketdataservice.shared.provider.tcmb.BondEodPoint;
import com.company.marketdataservice.shared.provider.tcmb.TcmbBondEvdsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * `makro oran` application katmanı use-case servisi.
 */
@Service
public class CpiMonthlySyncService {

    private static final Logger log = LoggerFactory.getLogger(CpiMonthlySyncService.class);
    private static final ZoneId TR = ZoneId.of("Europe/Istanbul");
    private static final MathContext MC = MathContext.DECIMAL64;

    private final MdsCpiMonthlyRepository cpiRepository;
    private final TcmbBondEvdsClient tcmbBondEvdsClient;
    private final MarketEvdsProperties evdsProperties;
    private final AtomicBoolean asyncKickoffInFlight = new AtomicBoolean(false);

    public CpiMonthlySyncService(
            MdsCpiMonthlyRepository cpiRepository,
            TcmbBondEvdsClient tcmbBondEvdsClient,
            MarketEvdsProperties evdsProperties
    ) {
        this.cpiRepository = cpiRepository;
        this.tcmbBondEvdsClient = tcmbBondEvdsClient;
        this.evdsProperties = evdsProperties;
    }

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    public void requestInitialSyncIfEmptyAsync() {
        if (cpiRepository.countByMetric(CpiMetric.INDEX) > 0) {
            return;
        }
        if (!asyncKickoffInFlight.compareAndSet(false, true)) {
            return;
        }
        Thread.ofVirtual()
                .name("mds-cpi-monthly-initial")
                .start(() -> {
                    try {
                        syncIfNeeded();
                    } catch (Exception ex) {
                        log.warn("CPI_MONTHLY_INITIAL_SYNC_FAILED reason={}", ex.toString());
                    } finally {
                        asyncKickoffInFlight.set(false);
                    }
                });
    }

    /**
     * Harici kaynaktan veriyi senkronize eder.
         */
    public void syncIfNeeded() {
        if (!StringUtils.hasText(evdsProperties.getApiKey())) {
            log.warn("CPI_MONTHLY_SYNC_SKIP no EVDS apiKey");
            return;
        }
        String series = evdsProperties.getCpiIndexSeries();
        if (!StringUtils.hasText(series)) {
            log.warn("CPI_MONTHLY_SYNC_SKIP blank cpiIndexSeries");
            return;
        }

        LocalDate rangeEnd = LocalDate.now(TR);
        LocalDate rangeStart = rangeEnd.minusYears(5).withDayOfMonth(1);
        String freq = StringUtils.hasText(evdsProperties.getCpiEvdsFrequency())
                ? evdsProperties.getCpiEvdsFrequency().trim()
                : "5";

        log.info("CPI_MONTHLY_SYNC_START window={}..{} series={}", rangeStart, rangeEnd, series.trim());

        List<BondEodPoint> raw;
        try {
            raw = tcmbBondEvdsClient.fetchHistoryPoints(series.trim(), rangeStart, rangeEnd, freq);
        } catch (Exception ex) {
            log.warn("CPI_MONTHLY_SYNC_EVDS_FAILED reason={}", ex.toString(), ex);
            return;
        }

        Map<LocalDate, BigDecimal> indexByMonth = raw.stream()
                .filter(p -> p.value() != null && p.value().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toMap(
                        p -> p.date().withDayOfMonth(1),
                        BondEodPoint::value,
                        (a, b) -> b
                ));

        List<LocalDate> months = new ArrayList<>(indexByMonth.keySet());
        months.sort(Comparator.naturalOrder());

        Instant now = Instant.now();
        for (LocalDate month : months) {
            if (month.isBefore(rangeStart) || month.isAfter(rangeEnd)) {
                continue;
            }
            upsert(CpiMetric.INDEX, month, indexByMonth.get(month), now);
        }

        for (LocalDate month : months) {
            BigDecimal cur = indexByMonth.get(month);
            LocalDate prevMonth = month.minusMonths(1);
            BigDecimal prev = indexByMonth.get(prevMonth);
            if (prev != null && prev.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal mom = cur.subtract(prev, MC)
                        .divide(prev, MC)
                        .multiply(BigDecimal.valueOf(100), MC)
                        .setScale(4, RoundingMode.HALF_UP);
                upsert(CpiMetric.MONTHLY_PCT, month, mom, now);
            }

            LocalDate yoyMonth = month.minusYears(1);
            BigDecimal yearAgo = indexByMonth.get(yoyMonth);
            if (yearAgo != null && yearAgo.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal yoy = cur.subtract(yearAgo, MC)
                        .divide(yearAgo, MC)
                        .multiply(BigDecimal.valueOf(100), MC)
                        .setScale(4, RoundingMode.HALF_UP);
                upsert(CpiMetric.YEARLY_PCT, month, yoy, now);
            }
        }

        log.info("CPI_MONTHLY_SYNC_DONE indexMonths={}", months.size());
    }

    private void upsert(CpiMetric metric, LocalDate monthStart, BigDecimal value, Instant ingestTime) {
        MdsCpiMonthlyEntity row = cpiRepository
                .findByMetricAndMonthStart(metric, monthStart)
                .orElseGet(MdsCpiMonthlyEntity::new);
        row.setMetric(metric);
        row.setMonthStart(monthStart);
        row.setValue(value);
        row.setSourceProvider("TCMB_EVDS");
        row.setIngestTime(ingestTime);
        cpiRepository.save(row);
    }
}
