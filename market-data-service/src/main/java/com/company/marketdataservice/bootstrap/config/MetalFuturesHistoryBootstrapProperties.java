package com.company.marketdataservice.bootstrap.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * `uygulama bootstrap` feature yapılandırma property'leri (`application.yml` prefix).
 */
@Component
@ConfigurationProperties(prefix = "market.metal-futures.history-bootstrap")
public class MetalFuturesHistoryBootstrapProperties {

    private boolean enabled = true;
    private long startupDelayMs = 90_000L;
    private int years = 5;
    private long periodicDelayMs = 3_600_000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getStartupDelayMs() {
        return startupDelayMs;
    }

    public void setStartupDelayMs(long startupDelayMs) {
        this.startupDelayMs = startupDelayMs;
    }

    public int getYears() {
        return years;
    }

    public void setYears(int years) {
        this.years = years;
    }

    public long getPeriodicDelayMs() {
        return periodicDelayMs;
    }

    public void setPeriodicDelayMs(long periodicDelayMs) {
        this.periodicDelayMs = periodicDelayMs;
    }
}
