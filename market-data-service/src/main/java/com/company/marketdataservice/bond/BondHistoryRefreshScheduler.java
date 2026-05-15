package com.company.marketdataservice.bond;

import com.company.marketdataservice.config.TcmbBondMarketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Keeps {@code mds_market_price_history} TRBOND rows aligned with EVDS daily pulls + calendar forward-fill.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "market.bond.history-refresh", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BondHistoryRefreshScheduler {

    private final BondHistoryBackfillService bondHistoryBackfillService;

    @Scheduled(cron = "${market.bond.history-refresh.cron:0 15 8 * * *}", zone = "Europe/Istanbul")
    public void refreshTrailingBondHistory() {
        try {
            bondHistoryBackfillService.refreshTrailingWindow();
        } catch (Exception ex) {
            log.warn("BOND_HISTORY_REFRESH_SCHEDULE_FAILED reason={}", ex.toString(), ex);
        }
    }
}
