package com.company.marketdataservice.bootstrap.config;
import com.company.marketdataservice.fund.infrastructure.scheduler.FundScheduler;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * `uygulama bootstrap` feature yapılandırma property'leri (`application.yml` prefix).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "market.fund")
public class FundMarketProperties {

    private boolean schedulerEnabled;

    private long delayMs = 3_600_000L;

    /** First {@link com.company.marketdataservice.fund.infrastructure.scheduler.FundScheduler} run after startup (ms). */
    private long schedulerInitialDelayMs = 120_000L;

    private List<String> trackedFundCodes = new ArrayList<>();

    private String httpUrlTemplate = "";

    private String tefasBindHistoryUrl = "https://www.tefas.gov.tr/api/DB/BindHistoryInfo";

    /**
     * TEFAS Next.js JSON API (replaces legacy {@code /api/DB/BindHistoryInfo} form POST for most deployments).
     */
    private String tefasFonGnlBlgUrl = "https://www.tefas.gov.tr/api/funds/fonGnlBlgSiraliGetir";

    /** Max calendar days per {@code fonGnlBlgSiraliGetir} request (TEFAS ~1 month cap). */
    private int tefasFonGnlChunkDays = 28;

    /** Pause between JSON chunk calls (TEFAS rate limit is roughly 6 requests/minute). */
    private long tefasFonGnlRequestSpacingMs = 10_000L;

    private String tefasFontip = "YAT";

    /** Total history window stitched from <=90-day API segments. */
    private int tefasHistoryLookbackDays = 370;

    /** BindHistory rejects requests when bastarih-bittarih span exceeds TEFAS policy (~90 calendar days). */
    private int tefasHistoryChunkInclusiveDays = 89;

    /**
     * When {@code market.history.backfill} runs for asset FUND, initial window depth (years) — independent of global
     * {@code market.history.backfill.years} used for equities.
     */
    private int historicalNavBackfillYears = 1;

    private NavHistoryBootstrap navHistoryBootstrap = new NavHistoryBootstrap();

    @Getter
    @Setter
    public static class NavHistoryBootstrap {
        /** One-shot NAV history load into {@code mds_fund_nav_history} after app startup (docker-friendly). */
        private boolean enabled = false;
        private long delayMs = 45_000L;
        private int lookbackDays = 365;
    }
}
