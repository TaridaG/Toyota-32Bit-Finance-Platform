package com.company.marketdataservice.rates.infrastructure.bootstrap;

import com.company.marketdataservice.rates.application.RepoRateSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "market.repo-rate.sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RepoRateBootstrapListener {

    private final RepoRateSyncService repoRateSyncService;

    @Value("${market.repo-rate.sync.startup-delay-ms:50000}")
    private long startupDelayMs;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        long delay = Math.max(0L, startupDelayMs);
        Thread.ofVirtual()
                .name("mds-repo-rate-bootstrap")
                .start(() -> {
                    try {
                        Thread.sleep(delay);
                        repoRateSyncService.syncIfNeeded();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.warn("REPO_RATE_BOOTSTRAP_INTERRUPTED");
                    } catch (Exception ex) {
                        log.warn("REPO_RATE_BOOTSTRAP_FAILED reason={}", ex.toString(), ex);
                    }
                });
    }

    @Scheduled(cron = "${market.repo-rate.sync.weekly-cron:0 22 7 * * MON}", zone = "Europe/Istanbul")
    public void scheduledWeeklyCheck() {
        try {
            repoRateSyncService.syncIfNeeded();
        } catch (Exception ex) {
            log.warn("REPO_RATE_SCHEDULE_FAILED reason={}", ex.toString(), ex);
        }
    }
}
