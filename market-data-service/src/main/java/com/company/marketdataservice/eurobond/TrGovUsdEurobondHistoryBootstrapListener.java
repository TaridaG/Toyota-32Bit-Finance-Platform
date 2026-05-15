package com.company.marketdataservice.eurobond;

import com.company.marketdataservice.config.TrGovUsdEurobondProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "market.tr-gov-usd-eurobond.history-bootstrap", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TrGovUsdEurobondHistoryBootstrapListener {

    private final TrGovUsdEurobondProperties properties;
    private final TrGovUsdEurobondHistoryService trGovUsdEurobondHistoryService;

    @EventListener(ApplicationReadyEvent.class)
    public void scheduleEurobondHistoryBootstrap() {
        long delay = Math.max(0L, properties.getHistoryBootstrap().getDelayMs());
        Thread.ofVirtual()
                .name("tr-gov-usd-eurobond-history-bootstrap")
                .start(() -> {
                    try {
                        Thread.sleep(delay);
                        trGovUsdEurobondHistoryService.backfillFromYahoo();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        log.warn("TRGOVUSD_BOOTSTRAP_INTERRUPTED");
                    } catch (Exception ex) {
                        log.warn("TRGOVUSD_BOOTSTRAP_FAILED reason={}", ex.toString(), ex);
                    }
                });
    }
}
