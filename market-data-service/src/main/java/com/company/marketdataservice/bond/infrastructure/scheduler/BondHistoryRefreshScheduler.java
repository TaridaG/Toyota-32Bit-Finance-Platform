package com.company.marketdataservice.bond.infrastructure.scheduler;
import com.company.marketdataservice.bond.application.BondHistoryBackfillService;
import com.company.marketdataservice.bootstrap.config.TcmbBondMarketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * {@code TRBOND*} geçmiş fiyat tablosunda trailing pencereyi cron ile yenileyen scheduler.
 * EVDS verilerini {@link BondHistoryBackfillService#refreshTrailingWindow()} ile DB'ye merge ederek aktarır birleştirir;
 * Kafka publish yoktur.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "market.bond.history-refresh", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BondHistoryRefreshScheduler {

    private final BondHistoryBackfillService bondHistoryBackfillService;

    /**
     * Günlük cron ile son lookback günlerini EVDS'ten tekrar çekip {@code mds_market_price_history}'ye idempotent upsert eder.
     */
    @Scheduled(cron = "${market.bond.history-refresh.cron:0 15 8 * * *}", zone = "Europe/Istanbul")
    public void refreshTrailingBondHistory() {
        try {
            bondHistoryBackfillService.refreshTrailingWindow();
        } catch (Exception ex) {
            log.warn("BOND_HISTORY_REFRESH_SCHEDULE_FAILED reason={}", ex.toString(), ex);
        }
    }
}
