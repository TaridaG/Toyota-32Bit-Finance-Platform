package com.company.marketdataservice.rates.infrastructure.bootstrap;
import com.company.marketdataservice.rates.application.CpiMonthlySyncService;
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
@ConditionalOnProperty(prefix = "market.cpi.monthly-sync", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CpiMonthlyBootstrapListener {

    private final CpiMonthlySyncService cpiMonthlySyncService;

    @Value("${market.cpi.monthly-sync.startup-delay-ms:55000}")
    private long startupDelayMs;

    /**
     * İş mantığı operasyonunu çalıştırır.
         */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        long delay = Math.max(0L, startupDelayMs);
        Thread.ofVirtual()
                .name("mds-cpi-monthly-bootstrap")
                .start(() -> {
                    try {
                        Thread.sleep(delay);
                        cpiMonthlySyncService.syncIfNeeded();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.warn("CPI_MONTHLY_BOOTSTRAP_INTERRUPTED");
                    } catch (Exception ex) {
                        log.warn("CPI_MONTHLY_BOOTSTRAP_FAILED reason={}", ex.toString(), ex);
                    }
                });
    }

    /**
     * Zamanlanmış işi tetikler.
         */
    @Scheduled(cron = "${market.cpi.monthly-sync.monthly-cron:0 35 8 5 * *}", zone = "Europe/Istanbul")
    public void scheduledMonthlyRefresh() {
        try {
            cpiMonthlySyncService.syncIfNeeded();
        } catch (Exception ex) {
            log.warn("CPI_MONTHLY_SCHEDULE_FAILED reason={}", ex.toString(), ex);
        }
    }
}
