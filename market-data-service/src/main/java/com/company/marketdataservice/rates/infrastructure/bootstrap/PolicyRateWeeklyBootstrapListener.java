package com.company.marketdataservice.rates.infrastructure.bootstrap;
import com.company.marketdataservice.rates.application.PolicyRateWeeklySyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


/**
 * `makro oran` için uygulama açılışında veya gecikmeli tetiklenen bootstrap listener.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "market.policy-rate.weekly-sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PolicyRateWeeklyBootstrapListener {

    private final PolicyRateWeeklySyncService policyRateWeeklySyncService;

    @Value("${market.policy-rate.weekly-sync.startup-delay-ms:45000}")
    private long startupDelayMs;

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        long delay = Math.max(0L, startupDelayMs);
        Thread.ofVirtual()
                .name("mds-policy-rate-weekly-bootstrap")
                .start(() -> {
                    try {
                        Thread.sleep(delay);
                        policyRateWeeklySyncService.syncIfNeeded();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.warn("POLICY_RATE_WEEKLY_BOOTSTRAP_INTERRUPTED");
                    } catch (Exception ex) {
                        log.warn("POLICY_RATE_WEEKLY_BOOTSTRAP_FAILED reason={}", ex.toString(), ex);
                    }
                });
    }

    /**
     * Zamanlanmış işi tetikler.
         */
    @Scheduled(cron = "${market.policy-rate.weekly-sync.weekly-cron:0 20 7 * * MON}", zone = "Europe/Istanbul")
    public void scheduledWeeklyCheck() {
        try {
            policyRateWeeklySyncService.syncIfNeeded();
        } catch (Exception ex) {
            log.warn("POLICY_RATE_WEEKLY_SCHEDULE_FAILED reason={}", ex.toString(), ex);
        }
    }
}
