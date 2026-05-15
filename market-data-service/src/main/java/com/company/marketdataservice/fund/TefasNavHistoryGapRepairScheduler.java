package com.company.marketdataservice.fund;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically extends {@code mds_fund_nav_history} when the one-shot bootstrap or TEFAS chunk calls left a forward
 * gap (e.g. last row in November while today is May).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "market.fund.nav-gap-repair.enabled", havingValue = "true", matchIfMissing = false)
public class TefasNavHistoryGapRepairScheduler {

    private final TefasFundNavHistoryRehydrationService rehydrationService;

    /** First run 15 minutes after startup, then every 24 hours (avoids racing the delayed bootstrap thread). */
    @Scheduled(initialDelayString = "${market.fund.nav-gap-repair.initial-delay-ms:900000}", fixedDelayString = "${market.fund.nav-gap-repair.fixed-delay-ms:86400000}")
    public void scheduledGapRepair() {
        try {
            rehydrationService.repairTrackedFundNavGaps();
        } catch (Exception ex) {
            log.warn("TEFAS_NAV_GAP_REPAIR_SCHEDULED_FAILED reason={}", ex.toString());
        }
    }
}
