package com.company.marketdataservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "market.bond")
public class TcmbBondMarketProperties {

    private boolean schedulerEnabled = true;
    private List<TcmbBondSeries> tracked = new ArrayList<>();
    private BondHistoryBootstrap historyBootstrap = new BondHistoryBootstrap();
    private BondHistoryRefresh historyRefresh = new BondHistoryRefresh();

    @Getter
    @Setter
    public static class BondHistoryBootstrap {
        /**
         * Load EVDS daily yields into {@code mds_market_price_history} once after startup (virtual thread).
         */
        private boolean enabled = true;
        private long delayMs = 120_000L;
        private int lookbackYears = 5;
        private int chunkDays = 400;
        private long chunkSpacingMs = 400L;
        /**
         * EVDS {@code frequency} query param (e.g. {@code 1} = daily). Empty = omit param (EVDS default).
         */
        private String evdsFrequency = "1";
        /** When true, expand sparse EVDS prints to every calendar day (last value carried forward). */
        private boolean forwardFillCalendarDays = true;
    }

    @Getter
    @Setter
    public static class BondHistoryRefresh {
        /**
         * Re-fetch a short trailing window with the same daily + forward-fill rules so new TCMB prints appear
         * without waiting for a full history re-bootstrap.
         */
        private boolean enabled = true;
        private int lookbackDays = 60;
        private long chunkSpacingMs = 400L;
    }

    @Getter
    @Setter
    public static class TcmbBondSeries {
        private String symbol;
        private String evdsSeries;
    }
}
