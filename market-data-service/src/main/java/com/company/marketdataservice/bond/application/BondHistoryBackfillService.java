package com.company.marketdataservice.bond.application;
import com.company.marketdataservice.bond.domain.BondHistoryForwardFill;
import com.company.marketdataservice.bootstrap.config.TcmbBondMarketProperties;
import com.company.marketdataservice.spot.domain.MarketPriceUpdatedEvent;
import com.company.marketdataservice.history.infrastructure.persistence.MarketPriceHistoryRepository;
import com.company.marketdataservice.catalog.application.InstrumentMappingService;
import com.company.marketdataservice.shared.provider.tcmb.BondEodPoint;
import com.company.marketdataservice.shared.provider.tcmb.TcmbBondEvdsClient;
import com.company.marketdataservice.history.infrastructure.write.MarketHistoryWriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;


/**
 * {@code tahvil verileri (TCMB EVDS)} geçmiş fiyat backfill ve trailing refresh application servisidir
 * proje ilk başlatıldıgında geçmiş verileri çekmek için ayarlar
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BondHistoryBackfillService {

    private static final String SOURCE = "TCMB_BOND";
    private static final String PRICE_TYPE = "MARKET";

    private final TcmbBondMarketProperties bondProperties;
    private final TcmbBondEvdsClient tcmbBondEvdsClient;
    private final InstrumentMappingService instrumentMappingService;
    private final MarketHistoryWriteService marketHistoryWriteService;
    private final MarketPriceHistoryRepository marketPriceHistoryRepository;

    /**
     * {@link TcmbBondMarketProperties#getTracked()} serileri için yapılandırılmış lookback yılı kadar EVDS geçmişini
     * chunk'lar halinde fetch edip forward-fill sonrası {@code mds_market_price_history}'ye persist eder.
     */
    public void backfillTrackedBonds() {
        List<TcmbBondMarketProperties.TcmbBondSeries> tracked = bondProperties.getTracked();
        if (tracked == null || tracked.isEmpty()) {
            log.info("BOND_HISTORY_BACKFILL_SKIP reason=no_tracked_series");
            return;
        }
        TcmbBondMarketProperties.BondHistoryBootstrap cfg = bondProperties.getHistoryBootstrap();
        int years = Math.max(1, Math.min(30, cfg.getLookbackYears()));
        int chunkDays = Math.max(30, Math.min(800, cfg.getChunkDays()));
        long spacing = Math.max(0L, cfg.getChunkSpacingMs());
        String freq = resolveEvdsFrequency(cfg.getEvdsFrequency());
        boolean forwardFill = cfg.isForwardFillCalendarDays();

        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = end.minusYears(years);

        for (TcmbBondMarketProperties.TcmbBondSeries row : tracked) {
            if (row == null || !StringUtils.hasText(row.getSymbol()) || !StringUtils.hasText(row.getEvdsSeries())) {
                continue;
            }
            String symbol = row.getSymbol().trim().toUpperCase(Locale.ROOT);
            String evdsSeries = row.getEvdsSeries().trim();
            Long instrumentId = instrumentMappingService.resolveInstrument(SOURCE, symbol).orElse(null);
            try {
                int totalPoints = 0;
                LocalDate chunkStart = start;
                while (!chunkStart.isAfter(end)) {
                    LocalDate chunkEnd = chunkStart.plusDays(chunkDays - 1L);
                    if (chunkEnd.isAfter(end)) {
                        chunkEnd = end;
                    }
                    int n = mergeChunk(symbol, evdsSeries, chunkStart, chunkEnd, freq, forwardFill, instrumentId);
                    totalPoints += n;
                    if (spacing > 0L) {
                        Thread.sleep(spacing);
                    }
                    chunkStart = chunkEnd.plusDays(1);
                }
                log.info("BOND_HISTORY_BACKFILL_DONE symbol={} evdsSeries={} pointsApprox={}", symbol, evdsSeries, totalPoints);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("BOND_HISTORY_BACKFILL_INTERRUPTED symbol={}", symbol);
                return;
            } catch (Exception ex) {
                log.warn("BOND_HISTORY_BACKFILL_FAIL symbol={} evdsSeries={} reason={}", symbol, evdsSeries, ex.toString());
            }
        }
    }

    /**
     * Her aktif olarak takip edilen tahvil için son {@code lookbackDays} takvim penceresini EVDS'ten yeniden merge ediyor.
     * Scheduled refresh; unique constraint ile idempotent upsert.
     * Cron ile, config’teki her tahvil serisi için son N günü TCMB EVDS’ten tekrar çekip geçmiş fiyat tablosuna yazar;
     * aynı gün veya sembol için tekrar yazınca unique key sayesinde tekrarlayan kayıt oluşmuyor
     */
    public void refreshTrailingWindow() {
        TcmbBondMarketProperties.BondHistoryRefresh refresh = bondProperties.getHistoryRefresh();
        if (refresh == null || !refresh.isEnabled()) {
            return;
        }
        List<TcmbBondMarketProperties.TcmbBondSeries> tracked = bondProperties.getTracked();
        if (tracked == null || tracked.isEmpty()) {
            return;
        }
        TcmbBondMarketProperties.BondHistoryBootstrap boot = bondProperties.getHistoryBootstrap();
        String freq = resolveEvdsFrequency(boot.getEvdsFrequency());
        boolean forwardFill = boot.isForwardFillCalendarDays();
        int lookback = Math.max(7, Math.min(800, refresh.getLookbackDays()));
        long spacing = Math.max(0L, refresh.getChunkSpacingMs());

        LocalDate end = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = end.minusDays(lookback - 1L);

        for (TcmbBondMarketProperties.TcmbBondSeries row : tracked) {
            if (row == null || !StringUtils.hasText(row.getSymbol()) || !StringUtils.hasText(row.getEvdsSeries())) {
                continue;
            }
            String symbol = row.getSymbol().trim().toUpperCase(Locale.ROOT);
            String evdsSeries = row.getEvdsSeries().trim();
            Long instrumentId = instrumentMappingService.resolveInstrument(SOURCE, symbol).orElse(null);
            try {
                int n = mergeChunk(symbol, evdsSeries, start, end, freq, forwardFill, instrumentId);
                log.info("BOND_HISTORY_REFRESH_DONE symbol={} evdsSeries={} window={}..{} pointsApprox={}", symbol, evdsSeries, start, end, n);
                if (spacing > 0L) {
                    Thread.sleep(spacing);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("BOND_HISTORY_REFRESH_INTERRUPTED symbol={}", symbol);
                return;
            } catch (Exception ex) {
                log.warn("BOND_HISTORY_REFRESH_FAIL symbol={} evdsSeries={} reason={}", symbol, evdsSeries, ex.toString());
            }
        }
    }

    /**
     * Tek tarih aralığı: EVDS fetch ->> isteğe bağlı forward-fill -> {@link MarketHistoryWriteService#saveBatch}.
     * tek tarih aralığı derken chunk başlama ve bitiş aralıgı örnek olarak 90 gün gibi.
     * @return persist edilen nokta sayısı
     */
    private int mergeChunk(
            String symbol,
            String evdsSeries,
            LocalDate chunkStart,
            LocalDate chunkEnd,
            String evdsFrequency,
            boolean forwardFill,
            Long instrumentId
    ) {
        List<BondEodPoint> sparse = tcmbBondEvdsClient.fetchHistoryPoints(evdsSeries, chunkStart, chunkEnd, evdsFrequency);
        List<BondEodPoint> toStore;
        if (forwardFill) {
            Instant beforeChunk = chunkStart.atStartOfDay(ZoneOffset.UTC).toInstant();
            BigDecimal seed = findSeedBefore(symbol, beforeChunk);
            toStore = BondHistoryForwardFill.expand(sparse, chunkStart, chunkEnd, seed);
        } else {
            toStore = sparse == null ? List.of() : sparse;
        }
        if (toStore.isEmpty()) {
            return 0;
        }
        List<MarketPriceUpdatedEvent> events = new ArrayList<>(toStore.size());
        for (BondEodPoint p : toStore) {
            Instant at = p.date().atStartOfDay(ZoneOffset.UTC).toInstant();
            events.add(MarketPriceUpdatedEvent.ofAt(symbol, p.value(), PRICE_TYPE, SOURCE, instrumentId, at));
        }
        marketHistoryWriteService.saveBatch(events);
        return events.size();
    }

    /**
     * Chunk başlangıcından önceki son bilinen fiyat; parçalı backfill'de forward-fill seed'i.
     */
    private BigDecimal findSeedBefore(String symbol, Instant beforeExclusive) {
        List<BigDecimal> rows = marketPriceHistoryRepository.findLatestPricesBefore(
                symbol,
                SOURCE,
                PRICE_TYPE,
                beforeExclusive,
                PageRequest.of(0, 1)
        );
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        return Optional.ofNullable(rows.get(0)).orElse(null);
    }

    /** EVDS {@code frequency} query parametresi; boşsa {@code null} (parametre gönderilmez). */
    private static String resolveEvdsFrequency(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        return raw.trim();
    }
}
