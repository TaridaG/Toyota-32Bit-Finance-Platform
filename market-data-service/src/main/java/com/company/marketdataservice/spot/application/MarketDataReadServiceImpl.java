package com.company.marketdataservice.spot.application;
import com.company.marketdataservice.catalog.domain.MarketCatalogSegmentRules;
import com.company.marketdataservice.fx.domain.FxQuoteNormalization;
import com.company.marketdataservice.bootstrap.config.HotReadCacheProperties;
import com.company.marketdataservice.bootstrap.config.TcmbBondMarketProperties;
import com.company.marketdataservice.shared.cache.JsonCacheService;
import com.company.marketdataservice.shared.cache.JsonCacheSupport;
import com.company.marketdataservice.spot.infrastructure.http.dto.FundDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.FxRateDto;
import com.company.marketdataservice.spot.infrastructure.http.dto.MarketPriceDto;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryEntry;
import com.company.marketdataservice.history.infrastructure.persistence.FundNavHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.FxRateHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository.LatestMarketPriceView;
import com.company.marketdataservice.shared.provider.tcmb.TcmbBondEvdsClient;
import com.company.marketdataservice.spot.infrastructure.snapshot.MarketSnapshotStore;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Spot fiyat, FX, fon NAV ve tahvil snapshot'larını birleşik REST read model'i olarak sunar.
 */
@Service
@RequiredArgsConstructor
public class MarketDataReadServiceImpl implements MarketDataReadService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataReadServiceImpl.class);

    private final MarketSnapshotStore snapshotStore;
    private final MarketPriceHistoryRepository marketPriceHistoryRepository;
    private final FxRateHistoryRepository fxRateHistoryRepository;
    private final FundNavHistoryRepository fundNavHistoryRepository;
    private final TcmbBondMarketProperties bondMarketProperties;
    private final TcmbBondEvdsClient tcmbBondEvdsClient;
    private final JsonCacheService jsonCacheService;
    private final HotReadCacheProperties hotReadCacheProperties;

    /** Kısa TTL: DB geçmişi boşken EVDS yapılandırılmışsa katalog satırlarını doldurur. */
    private volatile List<MarketPriceDto> bondEvdsOverlayCache = List.of();
    private volatile long bondEvdsOverlayExpiresMs;

    /**
     * Veriyi okur ve döner.
         * @return işlem sonucu
         */
    @Override
    public List<MarketPriceDto> getLatestPrices() {
        return getLatestPrices(null);
    }

    /**
     * Veriyi okur ve döner.
         * @param segment girdi parametresi
         * @return işlem sonucu
         */
    @Override
    public List<MarketPriceDto> getLatestPrices(String segment) {
        if (!hotReadCacheProperties.isEnabled()) {
            return loadLatestPricesUncached(segment);
        }
        return JsonCacheSupport.getOrLoad(
                jsonCacheService,
                pricesCacheKey(segment),
                Duration.ofMillis(hotReadCacheProperties.getPricesTtlMs()),
                new TypeReference<>() {},
                () -> loadLatestPricesUncached(segment));
    }

    private List<MarketPriceDto> loadLatestPricesUncached(String segment) {
        LinkedHashMap<String, MarketPriceDto> merged = new LinkedHashMap<>();
        for (MarketPriceDto p : snapshotStore.listPrices()) {
            merged.put(norm(p.symbol()), p);
        }
        for (FundDto f : snapshotStore.listFunds()) {
            String sym = canonicalFundInstrumentSymbol(f.fundCode());
            if (!sym.isEmpty()) {
                merged.put(sym, MarketPriceDto.basic(sym, f.nav(), f.source(), f.timestamp()));
            }
        }
        for (FundNavHistoryEntry row : fundNavHistoryRepository.findLatestRowPerFundCode()) {
            String sym = canonicalFundInstrumentSymbol(row.getFundCode());
            if (!sym.isEmpty()) {
                merged.putIfAbsent(sym, MarketPriceDto.basic(sym, row.getNav(), row.getProvider(), row.getObservedAt()));
            }
        }
        /*
         * DB union: when the in-memory snapshot is empty, hydrate the full catalog from mds_market_price_history.
         * When the snapshot is warm, still merge TRBOND* rows — bonds often exist only in DB (EVDS backfill)
         * until the bond scheduler publishes live ticks into {@link MarketSnapshotStore}.
         */
        if (merged.isEmpty()) {
            List<MarketPriceDto> dbLatest = mapLatestPriceViews(marketPriceHistoryRepository.findLatestPricesPerSymbol());
            log.warn("MARKET_PRICE_SNAPSHOT_EMPTY dbLatestSymbols={}", dbLatest.size());
            for (MarketPriceDto p : dbLatest) {
                merged.putIfAbsent(norm(p.symbol()), p);
            }
        } else {
            for (MarketPriceDto p : mapLatestPriceViews(marketPriceHistoryRepository.findLatestTrbondPricesPerSymbol())) {
                merged.putIfAbsent(norm(p.symbol()), p);
            }
            for (MarketPriceDto p : mapLatestPriceViews(marketPriceHistoryRepository.findLatestCryptoPricesPerSymbol())) {
                merged.putIfAbsent(norm(p.symbol()), p);
            }
        }
        mergeTrackedBondsFromEvdsIfAbsent(merged);
        List<MarketPriceDto> sorted = sortBySymbol(merged);
        if (!StringUtils.hasText(segment)) {
            return sorted;
        }
        String want = segment.trim();
        return sorted.stream()
                .filter(p -> matchesPulseSegment(p, want))
                .toList();
    }

    private static boolean matchesPulseSegment(MarketPriceDto p, String requestedSegment) {
        if (p == null || p.symbol() == null || p.symbol().isBlank()) {
            return false;
        }
        String wire = MarketCatalogSegmentRules.inferWireCategory(p.symbol());
        String pulse = MarketCatalogSegmentRules.pulseSegment(p.symbol(), wire, p.source());
        return pulse != null && pulse.equalsIgnoreCase(requestedSegment.trim());
    }

    /**
     * When tracked {@code TRBOND*} rows are still missing (no DB history / snapshot tick yet), pull latest EVDS
     * points so the Markets SPA filter is not empty. Cached to avoid hammering TCMB on every poll.
     */
    private void mergeTrackedBondsFromEvdsIfAbsent(LinkedHashMap<String, MarketPriceDto> merged) {
        List<TcmbBondMarketProperties.TcmbBondSeries> tracked = bondMarketProperties.getTracked();
        if (tracked == null || tracked.isEmpty()) {
            return;
        }
        boolean anyMissing = false;
        for (TcmbBondMarketProperties.TcmbBondSeries row : tracked) {
            if (row == null || !StringUtils.hasText(row.getSymbol())) {
                continue;
            }
            if (!merged.containsKey(norm(row.getSymbol()))) {
                anyMissing = true;
                break;
            }
        }
        if (!anyMissing) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now >= bondEvdsOverlayExpiresMs) {
            synchronized (this) {
                if (System.currentTimeMillis() >= bondEvdsOverlayExpiresMs) {
                    bondEvdsOverlayCache = fetchLiveTrackedBondRows(tracked);
                    bondEvdsOverlayExpiresMs = System.currentTimeMillis() + 120_000L;
                }
            }
        }
        for (MarketPriceDto p : bondEvdsOverlayCache) {
            merged.putIfAbsent(norm(p.symbol()), p);
        }
    }

    private List<MarketPriceDto> fetchLiveTrackedBondRows(List<TcmbBondMarketProperties.TcmbBondSeries> tracked) {
        List<MarketPriceDto> out = new ArrayList<>();
        for (TcmbBondMarketProperties.TcmbBondSeries row : tracked) {
            if (row == null || !StringUtils.hasText(row.getSymbol()) || !StringUtils.hasText(row.getEvdsSeries())) {
                continue;
            }
            String sym = norm(row.getSymbol());
            try {
                BigDecimal price = tcmbBondEvdsClient.fetchLatestValue(row.getEvdsSeries());
                out.add(MarketPriceDto.basic(sym, price, "TCMB_BOND", Instant.now()));
            } catch (Exception ex) {
                log.warn("EVDS_LIVE_BOND_SKIP symbol={} series={} reason={}", sym, row.getEvdsSeries(), ex.toString());
            }
        }
        return List.copyOf(out);
    }

    private static List<MarketPriceDto> mapLatestPriceViews(List<LatestMarketPriceView> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> MarketPriceDto.basic(
                        row.getSymbol(),
                        row.getPrice(),
                        row.getSource(),
                        row.getTimestamp()))
                .toList();
    }

    private static List<MarketPriceDto> sortBySymbol(LinkedHashMap<String, MarketPriceDto> merged) {
        List<MarketPriceDto> out = new ArrayList<>(merged.values());
        out.sort(Comparator.comparing(MarketPriceDto::symbol, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    private static String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Aligns TEFAS {@code fundCode} (e.g. TI2) with finance {@code instruments.symbol} (FUND_TI2).
     */
    private static String pricesCacheKey(String segment) {
        if (!StringUtils.hasText(segment)) {
            return "mds:hot:prices:all";
        }
        return "mds:hot:prices:seg:" + segment.trim().toLowerCase(Locale.ROOT);
    }

    static String canonicalFundInstrumentSymbol(String fundCode) {
        String c = norm(fundCode);
        if (c.isEmpty()) {
            return "";
        }
        return c.startsWith("FUND_") ? c : "FUND_" + c;
    }

    /**
     * Veriyi okur ve döner.
         * @return işlem sonucu
         */
    @Override
    public List<FxRateDto> getFxRates() {
        if (!hotReadCacheProperties.isEnabled()) {
            return loadFxRatesUncached();
        }
        return JsonCacheSupport.getOrLoad(
                jsonCacheService,
                "mds:hot:fx",
                Duration.ofMillis(hotReadCacheProperties.getFxTtlMs()),
                new TypeReference<>() {},
                this::loadFxRatesUncached);
    }

    private List<FxRateDto> loadFxRatesUncached() {
        List<FxRateDto> live = snapshotStore.listFx();
        if (!live.isEmpty()) {
            return live;
        }
        return fxRateHistoryRepository.findLatestRatesPerSymbol()
                .stream()
                .map(row -> {
                    String sym = row.getCanonicalSymbol();
                    return new FxRateDto(
                            sym,
                            FxQuoteNormalization.normalizePrice(sym, row.getBid()),
                            FxQuoteNormalization.normalizePrice(sym, row.getAsk()),
                            FxQuoteNormalization.normalizePrice(sym, row.getMid()),
                            row.getSource(),
                            row.getObservedAt());
                })
                .toList();
    }

    /**
     * Veriyi okur ve döner.
         * @return işlem sonucu
         */
    @Override
    public List<FundDto> getFunds() {
        return snapshotStore.listFunds();
    }
}
