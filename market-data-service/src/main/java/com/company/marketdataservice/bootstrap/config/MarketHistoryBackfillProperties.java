package com.company.marketdataservice.bootstrap.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * `uygulama bootstrap` feature yapılandırma property'leri (`application.yml` prefix).
 */
@Component
@ConfigurationProperties(prefix = "market.history.backfill")
public class MarketHistoryBackfillProperties {

    private boolean enabled = false;
    private boolean runOnStartup = false;
    private Kafka kafka = new Kafka();
    private Retry retry = new Retry();
    private int years = 5;
    private int chunkDays = 30;
    private int startupDepthDays = 7;
    private int minPriceHistoryDays = 240;
    private int minRecentPriceHistoryDays = 120;
    private int minRecent30DayCoverageDays = 15;
    /** When false, live spot publish proceeds while history backfill runs in the background. */
    private boolean gateLiveUntilHistoryReady = false;
    /** Parallel stock history bootstrap workers (Yahoo/Finnhub historical fetches). */
    private int stockBootstrapParallelism = 4;
    private long sleepMs = 200L;
    private long scheduleInitialDelayMs = 30_000L;
    private long scheduleDelayMs = 900_000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRunOnStartup() {
        return runOnStartup;
    }

    public void setRunOnStartup(boolean runOnStartup) {
        this.runOnStartup = runOnStartup;
    }

    public int getYears() {
        return years;
    }

    public void setYears(int years) {
        this.years = years;
    }

    public int getChunkDays() {
        return chunkDays;
    }

    public void setChunkDays(int chunkDays) {
        this.chunkDays = chunkDays;
    }

    public int getStartupDepthDays() {
        return startupDepthDays;
    }

    public void setStartupDepthDays(int startupDepthDays) {
        this.startupDepthDays = startupDepthDays;
    }

    public int getMinPriceHistoryDays() {
        return minPriceHistoryDays;
    }

    public void setMinPriceHistoryDays(int minPriceHistoryDays) {
        this.minPriceHistoryDays = minPriceHistoryDays;
    }

    public int getMinRecentPriceHistoryDays() {
        return minRecentPriceHistoryDays;
    }

    public void setMinRecentPriceHistoryDays(int minRecentPriceHistoryDays) {
        this.minRecentPriceHistoryDays = minRecentPriceHistoryDays;
    }

    public int getMinRecent30DayCoverageDays() {
        return minRecent30DayCoverageDays;
    }

    public void setMinRecent30DayCoverageDays(int minRecent30DayCoverageDays) {
        this.minRecent30DayCoverageDays = minRecent30DayCoverageDays;
    }

    public boolean isGateLiveUntilHistoryReady() {
        return gateLiveUntilHistoryReady;
    }

    public void setGateLiveUntilHistoryReady(boolean gateLiveUntilHistoryReady) {
        this.gateLiveUntilHistoryReady = gateLiveUntilHistoryReady;
    }

    public int getStockBootstrapParallelism() {
        return stockBootstrapParallelism;
    }

    public void setStockBootstrapParallelism(int stockBootstrapParallelism) {
        this.stockBootstrapParallelism = stockBootstrapParallelism;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public long getSleepMs() {
        return sleepMs;
    }

    public void setSleepMs(long sleepMs) {
        this.sleepMs = sleepMs;
    }

    public long getScheduleInitialDelayMs() {
        return scheduleInitialDelayMs;
    }

    public void setScheduleInitialDelayMs(long scheduleInitialDelayMs) {
        this.scheduleInitialDelayMs = scheduleInitialDelayMs;
    }

    public long getScheduleDelayMs() {
        return scheduleDelayMs;
    }

    public void setScheduleDelayMs(long scheduleDelayMs) {
        this.scheduleDelayMs = scheduleDelayMs;
    }

    public static class Kafka {
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Retry {
        private long initialDelayMs = 1_000L;
        private double multiplier = 2.0d;
        private long maxDelayMs = 300_000L;
        private int maxAttempts = 8;

        public long getInitialDelayMs() {
            return initialDelayMs;
        }

        public void setInitialDelayMs(long initialDelayMs) {
            this.initialDelayMs = initialDelayMs;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }

        public long getMaxDelayMs() {
            return maxDelayMs;
        }

        public void setMaxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }
}
