package com.company.marketdataservice.bond.infrastructure.bootstrap;
import com.company.marketdataservice.bond.application.BondHistoryBackfillService;
import com.company.marketdataservice.bootstrap.config.TcmbBondMarketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;


/**
 * `tahvil` için uygulama açılışında veya gecikmeli tetiklenen bootstrap listener.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "market.bond.history-bootstrap", name = "enabled", havingValue = "true")
public class BondHistoryBootstrapListener {

    private final TcmbBondMarketProperties bondProperties;
    private final BondHistoryBackfillService bondHistoryBackfillService;

    /**
     * Zamanlanmış işi tetikler.
         */
    @EventListener(ApplicationReadyEvent.class)
    public void scheduleBondHistoryBootstrap() {
        long delay = Math.max(0L, bondProperties.getHistoryBootstrap().getDelayMs());
        Thread.ofVirtual()
                .name("tcmb-bond-history-bootstrap")
                .start(() -> {
                    try {
                        Thread.sleep(delay);
                        bondHistoryBackfillService.backfillTrackedBonds();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.warn("BOND_HISTORY_BOOTSTRAP_INTERRUPTED");
                    } catch (Exception ex) {
                        log.warn("BOND_HISTORY_BOOTSTRAP_FAILED reason={}", ex.toString(), ex);
                    }
                });
    }
}
