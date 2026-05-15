package com.company.finance_api.market.eurobond.yahoo;

import com.company.finance_api.config.MarketTrUsdEurobondYahooProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "market.tr-usd-eurobond-yahoo", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class EurobondYahooIngestStartupListener {

    private final MarketTrUsdEurobondYahooProperties properties;
    private final EurobondYahooHistoryIngestService ingestService;

    @EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!properties.isBackfillOnStartup()) {
            return;
        }
        try {
            ingestService.refreshAllConfigured();
        } catch (Exception ex) {
            log.warn("EUROBOND_YAHOO_STARTUP_REFRESH_FAIL reason={}", ex.toString());
        }
    }
}
