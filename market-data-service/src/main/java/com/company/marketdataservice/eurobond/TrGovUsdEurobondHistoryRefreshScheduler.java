package com.company.marketdataservice.eurobond;

import com.company.marketdataservice.config.TrGovUsdEurobondProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "market.tr-gov-usd-eurobond.history-refresh", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TrGovUsdEurobondHistoryRefreshScheduler {

    private final TrGovUsdEurobondHistoryService trGovUsdEurobondHistoryService;

    @Scheduled(cron = "${market.tr-gov-usd-eurobond.history-refresh.cron:0 28 8 * * *}", zone = "Europe/Istanbul")
    public void refresh() {
        try {
            trGovUsdEurobondHistoryService.refreshFromYahoo();
        } catch (Exception ex) {
            log.warn("TRGOVUSD_REFRESH_SCHEDULE_FAILED reason={}", ex.toString(), ex);
        }
    }
}
