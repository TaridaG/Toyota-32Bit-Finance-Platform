package com.company.marketdataservice.fund;

import com.company.marketdataservice.config.FundMarketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Optional delayed job: NAV gap repair (fast) then full lookback rehydrate (slow), then repair again.
 * Order matters: a full-year rehydrate can take a long time; running {@link TefasFundNavHistoryRehydrationService#repairTrackedFundNavGaps()}
 * first fixes forward/interior holes so UI metrics recover without waiting for the whole bootstrap.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "market.fund.nav-history-bootstrap", name = "enabled", havingValue = "true")
public class FundNavHistoryBootstrapListener {

    private final FundMarketProperties fundMarketProperties;
    private final TefasFundNavHistoryRehydrationService rehydrationService;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleNavHistoryBootstrap() {
        long delay = Math.max(0L, fundMarketProperties.getNavHistoryBootstrap().getDelayMs());
        Thread.ofVirtual()
                .name("tefas-nav-history-bootstrap")
                .start(() -> {
                    try {
                        Thread.sleep(delay);
                        rehydrationService.repairTrackedFundNavGaps();
                        Thread.sleep(3_000L);
                        rehydrationService.rehydrateTrackedFundsLastYear();
                        Thread.sleep(3_000L);
                        rehydrationService.repairTrackedFundNavGaps();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.warn("TEFAS_NAV_BOOTSTRAP_INTERRUPTED");
                    } catch (Exception ex) {
                        log.warn("TEFAS_NAV_BOOTSTRAP_FAILED reason={}", ex.toString(), ex);
                    }
                });
    }
}
