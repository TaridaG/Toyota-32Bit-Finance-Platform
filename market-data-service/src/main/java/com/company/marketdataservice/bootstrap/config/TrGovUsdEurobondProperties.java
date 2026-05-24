package com.company.marketdataservice.bootstrap.config;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


/**
 * `uygulama bootstrap` feature yapılandırma property'leri (`application.yml` prefix).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "market.tr-gov-usd-eurobond")
public class TrGovUsdEurobondProperties {

    private HistoryBootstrap historyBootstrap = new HistoryBootstrap();
    private HistoryRefresh historyRefresh = new HistoryRefresh();
    private List<TrackedSeries> tracked = new ArrayList<>();

    @Getter
    @Setter
    public static class HistoryBootstrap {
        private boolean enabled = true;
        private long delayMs = 150_000L;
        private String chartRange = "10y";
        private String chartInterval = "1d";
        private long chunkSpacingMs = 400L;
    }

    @Getter
    @Setter
    public static class HistoryRefresh {
        private boolean enabled = true;
        private String cron = "0 28 8 * * *";
        private String chartRange = "5y";
        private String chartInterval = "1d";
        private long chunkSpacingMs = 400L;
    }

    @Getter
    @Setter
    public static class TrackedSeries {
        /** Canonical instrument symbol stored in {@code mds_market_price_history.instrument_symbol}. */
        private String canonical;
        /** Yahoo chart symbol (often {@code GTUSDTR5Y:GOV}). */
        private String yahooChartSymbol;
    }
}
