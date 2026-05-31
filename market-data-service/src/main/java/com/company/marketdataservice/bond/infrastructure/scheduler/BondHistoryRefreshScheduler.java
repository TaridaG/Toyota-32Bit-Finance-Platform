package com.company.marketdataservice.bond.infrastructure.scheduler;
import com.company.marketdataservice.bond.application.BondHistoryBackfillService;
import com.company.marketdataservice.bootstrap.config.TcmbBondMarketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * `tahvil` verisini periyodik olarak fetch edip snapshot/Kafka'ya publish eden scheduler.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "market.bond.history-refresh", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BondHistoryRefreshScheduler {

    private final BondHistoryBackfillService bondHistoryBackfillService;

    /**
     * Önbelleği veya snapshot'ı yeniler.
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
