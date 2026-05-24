package com.company.marketdataservice.fund.infrastructure.scheduler;
import com.company.marketdataservice.fund.infrastructure.provider.TefasFundNavHistoryRehydrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * `fon (TEFAS NAV)` verisini periyodik olarak fetch edip snapshot/Kafka'ya publish eden scheduler.
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
