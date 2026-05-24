package com.company.marketdataservice.rates.infrastructure.bootstrap;
import com.company.marketdataservice.rates.application.TlDepositWeeklySyncService;
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
@ConditionalOnProperty(prefix = "market.tl-deposit.weekly-sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TlDepositWeeklyBootstrapListener {

    private final TlDepositWeeklySyncService tlDepositWeeklySyncService;

    @Value("${market.tl-deposit.weekly-sync.startup-delay-ms:60000}")
    private long startupDelayMs;

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        long delay = Math.max(0L, startupDelayMs);
        Thread.ofVirtual()
                .name("mds-tl-deposit-weekly-bootstrap")
                .start(() -> {
                    try {
                        Thread.sleep(delay);
                        tlDepositWeeklySyncService.syncIfNeeded();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.warn("TL_DEPOSIT_WEEKLY_BOOTSTRAP_INTERRUPTED");
                    } catch (Exception ex) {
                        log.warn("TL_DEPOSIT_WEEKLY_BOOTSTRAP_FAILED reason={}", ex.toString(), ex);
                    }
                });
    }

    /**
     * Zamanlanmış işi tetikler.
         */
    @Scheduled(cron = "${market.tl-deposit.weekly-sync.weekly-cron:0 25 7 * * MON}", zone = "Europe/Istanbul")
    public void scheduledWeeklyCheck() {
        try {
            tlDepositWeeklySyncService.syncIfNeeded();
        } catch (Exception ex) {
            log.warn("TL_DEPOSIT_WEEKLY_SCHEDULE_FAILED reason={}", ex.toString(), ex);
        }
    }
}
