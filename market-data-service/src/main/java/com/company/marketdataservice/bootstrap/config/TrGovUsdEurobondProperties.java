package com.company.marketdataservice.bootstrap.config;

import com.company.marketdataservice.catalog.registry.providers.EurobondRegistry;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TR USD eurobond history settings. Tracked series are defined in {@link EurobondRegistry}.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "market.tr-gov-usd-eurobond")
public class TrGovUsdEurobondProperties {

    private HistoryBootstrap historyBootstrap = new HistoryBootstrap();
    private HistoryRefresh historyRefresh = new HistoryRefresh();
    private List<TrackedSeries> tracked = new ArrayList<>();

    public List<TrackedSeries> getTracked() {
        if (tracked != null && !tracked.isEmpty()) {
            return tracked;
        }
        return EurobondRegistry.ingestRows().stream()
                .map(row -> {
                    TrackedSeries series = new TrackedSeries();
                    series.setCanonical(row.canonical());
                    series.setYahooChartSymbol(row.yahooChartSymbol());
                    return series;
                })
                .toList();
    }

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
        private String canonical;
        private String yahooChartSymbol;
    }
}
