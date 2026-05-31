package com.company.marketdataservice.bootstrap.config;

import com.company.marketdataservice.catalog.registry.providers.FundRegistry;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Fund market configuration (TEFAS HTTP settings). Tracked fund codes come from {@link FundRegistry}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "market.fund")
public class FundMarketProperties {

    private boolean schedulerEnabled;

    private long delayMs = 3_600_000L;

    private long schedulerInitialDelayMs = 120_000L;

    private String httpUrlTemplate = "";

    private String tefasBindHistoryUrl = "https://www.tefas.gov.tr/api/DB/BindHistoryInfo";

    private String tefasFonGnlBlgUrl = "https://www.tefas.gov.tr/api/funds/fonGnlBlgSiraliGetir";

    private int tefasFonGnlChunkDays = 28;

    private long tefasFonGnlRequestSpacingMs = 10_000L;

    private String tefasFontip = "YAT";

    private int tefasHistoryLookbackDays = 370;

    private int tefasHistoryChunkInclusiveDays = 89;

    private int historicalNavBackfillYears = 1;

    private NavHistoryBootstrap navHistoryBootstrap = new NavHistoryBootstrap();

    public java.util.List<String> getTrackedFundCodes() {
        return FundRegistry.tefasCodes();
    }

    @Getter
    @Setter
    public static class NavHistoryBootstrap {
        private boolean enabled = false;
        private long delayMs = 45_000L;
        private int lookbackDays = 365;
    }
}
