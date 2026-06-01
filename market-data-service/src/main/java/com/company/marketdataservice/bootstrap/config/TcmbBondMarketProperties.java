package com.company.marketdataservice.bootstrap.config;

import com.company.marketdataservice.catalog.registry.providers.BondRegistry;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * TCMB tahvil piyasası yapılandırması ({@code market.bond}): scheduler, EVDS geçmiş bootstrap ve trailing refresh.
 * Takip edilen seriler varsayılan olarak {@link BondRegistry} ingest satırlarından gelir.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "market.bond")
public class TcmbBondMarketProperties {

    private boolean schedulerEnabled = true;
    private List<TcmbBondSeries> tracked = new ArrayList<>();
    private BondHistoryBootstrap historyBootstrap = new BondHistoryBootstrap();
    private BondHistoryRefresh historyRefresh = new BondHistoryRefresh();

    /** YAML {@code tracked} doluysa onu; değilse {@link BondRegistry} ingest satırlarını döner. */
    public List<TcmbBondSeries> getTracked() {
        if (tracked != null && !tracked.isEmpty()) {
            return tracked;
        }
        return BondRegistry.ingestRows().stream()
                .map(row -> {
                    TcmbBondSeries series = new TcmbBondSeries();
                    series.setSymbol(row.symbol());
                    series.setEvdsSeries(row.evdsSeries());
                    return series;
                })
                .toList();
    }

    /** Uygulama açılışında EVDS geçmiş backfill (lookback yıl, chunk, forward-fill). */
    @Getter
    @Setter
    public static class BondHistoryBootstrap {
        private boolean enabled = true;
        private long delayMs = 120_000L;
        private int lookbackYears = 5;
        private int chunkDays = 400;
        private long chunkSpacingMs = 400L;
        private String evdsFrequency = "1";
        private boolean forwardFillCalendarDays = true;
    }

    /** Cron ile son N günü EVDS'ten yenileyen trailing refresh ayarları. */
    @Getter
    @Setter
    public static class BondHistoryRefresh {
        private boolean enabled = true;
        private int lookbackDays = 60;
        private long chunkSpacingMs = 400L;
        private String cron = "0 15 8 * * *";
    }

    @Getter
    @Setter
    public static class TcmbBondSeries {
        private String symbol;
        private String evdsSeries;
    }
}
