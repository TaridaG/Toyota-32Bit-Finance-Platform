package com.company.marketdataservice.eurobond;

import com.company.marketdataservice.config.TrGovUsdEurobondProperties;
import com.company.marketdataservice.event.MarketPriceUpdatedEvent;
import com.company.marketdataservice.instrument.InstrumentMappingService;
import com.company.marketdataservice.provider.yahoo.YahooFinanceClient;
import com.company.marketdataservice.provider.yahoo.YahooFinanceResponse;
import com.company.marketdataservice.service.history.MarketHistoryWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Ingests Turkey USD benchmark yields from Yahoo Finance (Bloomberg-style {@code GTUSDTR*n*Y:GOV} series)
 * into {@code mds_market_price_history} under canonical {@code TRGOVUSD*} symbols.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TrGovUsdEurobondHistoryService {

    private static final String SOURCE = "YAHOO";
    private static final String PRICE_TYPE = "MARKET";

    private final TrGovUsdEurobondProperties properties;
    private final YahooFinanceClient yahooFinanceClient;
    private final InstrumentMappingService instrumentMappingService;
    private final MarketHistoryWriteService marketHistoryWriteService;

    public void backfillFromYahoo() {
        List<TrGovUsdEurobondProperties.TrackedSeries> tracked = properties.getTracked();
        if (tracked == null || tracked.isEmpty()) {
            log.info("TRGOVUSD_HISTORY_SKIP reason=no_tracked_series");
            return;
        }
        long spacing = Math.max(0L, properties.getHistoryBootstrap().getChunkSpacingMs());
        String range = StringUtils.hasText(properties.getHistoryBootstrap().getChartRange())
                ? properties.getHistoryBootstrap().getChartRange().trim()
                : "10y";
        String interval = StringUtils.hasText(properties.getHistoryBootstrap().getChartInterval())
                ? properties.getHistoryBootstrap().getChartInterval().trim()
                : "1d";

        for (TrGovUsdEurobondProperties.TrackedSeries row : tracked) {
            if (row == null
                    || !StringUtils.hasText(row.getCanonical())
                    || !StringUtils.hasText(row.getYahooChartSymbol())) {
                continue;
            }
            String canonical = row.getCanonical().trim().toUpperCase(Locale.ROOT);
            String yahoo = row.getYahooChartSymbol().trim();
            Long instrumentId = instrumentMappingService.resolveInstrument(SOURCE, canonical).orElse(null);
            try {
                YahooFinanceResponse resp = yahooFinanceClient.fetchHistoricalChart(yahoo, range, interval);
                if (resp == null || resp.chart() == null || resp.chart().error() != null) {
                    log.warn(
                            "TRGOVUSD_YAHOO_EMPTY canonical={} yahoo={} err={}",
                            canonical,
                            yahoo,
                            resp != null && resp.chart() != null ? resp.chart().error() : null
                    );
                } else {
                    List<MarketPriceUpdatedEvent> events = buildCloseEvents(resp, canonical, instrumentId);
                    if (!events.isEmpty()) {
                        marketHistoryWriteService.saveBatch(events);
                        log.info("TRGOVUSD_YAHOO_DONE canonical={} yahoo={} points={}", canonical, yahoo, events.size());
                    } else {
                        log.warn("TRGOVUSD_YAHOO_NO_POINTS canonical={} yahoo={}", canonical, yahoo);
                    }
                }
            } catch (Exception ex) {
                log.warn("TRGOVUSD_YAHOO_FAIL canonical={} yahoo={} reason={}", canonical, yahoo, ex.toString());
            }
            if (spacing > 0L) {
                try {
                    Thread.sleep(spacing);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("TRGOVUSD_HISTORY_INTERRUPTED");
                    return;
                }
            }
        }
    }

    public void refreshFromYahoo() {
        List<TrGovUsdEurobondProperties.TrackedSeries> tracked = properties.getTracked();
        if (tracked == null || tracked.isEmpty()) {
            return;
        }
        long spacing = Math.max(0L, properties.getHistoryRefresh().getChunkSpacingMs());
        String range = StringUtils.hasText(properties.getHistoryRefresh().getChartRange())
                ? properties.getHistoryRefresh().getChartRange().trim()
                : "5y";
        String interval = StringUtils.hasText(properties.getHistoryRefresh().getChartInterval())
                ? properties.getHistoryRefresh().getChartInterval().trim()
                : "1d";

        for (TrGovUsdEurobondProperties.TrackedSeries row : tracked) {
            if (row == null
                    || !StringUtils.hasText(row.getCanonical())
                    || !StringUtils.hasText(row.getYahooChartSymbol())) {
                continue;
            }
            String canonical = row.getCanonical().trim().toUpperCase(Locale.ROOT);
            String yahoo = row.getYahooChartSymbol().trim();
            Long instrumentId = instrumentMappingService.resolveInstrument(SOURCE, canonical).orElse(null);
            try {
                YahooFinanceResponse resp = yahooFinanceClient.fetchHistoricalChart(yahoo, range, interval);
                if (resp != null && resp.chart() != null && resp.chart().error() == null) {
                    List<MarketPriceUpdatedEvent> events = buildCloseEvents(resp, canonical, instrumentId);
                    if (!events.isEmpty()) {
                        marketHistoryWriteService.saveBatch(events);
                        log.info("TRGOVUSD_YAHOO_REFRESH canonical={} points={}", canonical, events.size());
                    }
                }
            } catch (Exception ex) {
                log.warn("TRGOVUSD_YAHOO_REFRESH_FAIL canonical={} reason={}", canonical, ex.toString());
            }
            if (spacing > 0L) {
                try {
                    Thread.sleep(spacing);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private static List<MarketPriceUpdatedEvent> buildCloseEvents(
            YahooFinanceResponse response,
            String canonical,
            Long instrumentId
    ) {
        if (response.chart() == null
                || response.chart().result() == null
                || response.chart().result().isEmpty()) {
            return List.of();
        }
        YahooFinanceResponse.Result result = response.chart().result().get(0);
        if (result.timestamp() == null
                || result.timestamp().isEmpty()
                || result.indicators() == null
                || result.indicators().quote() == null
                || result.indicators().quote().isEmpty()) {
            return List.of();
        }
        YahooFinanceResponse.Quote quote = result.indicators().quote().get(0);
        List<Double> closes = quote.close();
        if (closes == null) {
            return List.of();
        }
        TreeMap<LocalDate, BigDecimal> byDay = new TreeMap<>();
        for (int i = 0; i < result.timestamp().size(); i++) {
            Long epoch = result.timestamp().get(i);
            if (epoch == null || i >= closes.size()) {
                continue;
            }
            Double c = closes.get(i);
            if (c == null || !Double.isFinite(c)) {
                continue;
            }
            Instant observed = Instant.ofEpochSecond(epoch);
            LocalDate day = observed.atZone(ZoneOffset.UTC).toLocalDate();
            byDay.put(day, BigDecimal.valueOf(c));
        }
        List<MarketPriceUpdatedEvent> out = new ArrayList<>(byDay.size());
        for (var e : byDay.entrySet()) {
            Instant at = e.getKey().atStartOfDay(ZoneOffset.UTC).toInstant();
            out.add(MarketPriceUpdatedEvent.ofAt(canonical, e.getValue(), PRICE_TYPE, SOURCE, instrumentId, at));
        }
        return out;
    }
}
