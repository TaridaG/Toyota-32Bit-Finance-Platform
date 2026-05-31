package com.company.finance_api.admin.application;

import com.company.finance_api.admin.infrastructure.http.dto.AdminMarketAssetDashboardDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminMarketAssetRecomputeResponseDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminMarketAssetStatRowDto;
import com.company.finance_api.admin.infrastructure.http.dto.AdminMarketAssetStatsPageDto;
import com.company.finance_api.admin.domain.AdminMarketAssetSnapshot;
import com.company.finance_api.admin.domain.AdminMarketAssetStat;
import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.portfolio.domain.Transaction;
import com.company.finance_api.portfolio.domain.PortfolioPosition;
import com.company.finance_api.portfolio.domain.PortfolioPositionBuilder;
import com.company.finance_api.portfolio.external.infrastructure.http.dto.ExternalPortfolioSummaryResponse;
import com.company.finance_api.portfolio.external.infrastructure.http.dto.ExternalPositionSummary;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPortfolioRepository;
import com.company.finance_api.portfolio.external.infrastructure.persistence.ExternalPositionLotRepository;
import com.company.finance_api.portfolio.external.application.ExternalPortfolioValuationService;
import com.company.finance_api.admin.infrastructure.persistence.AdminMarketAssetSnapshotRepository;
import com.company.finance_api.admin.infrastructure.persistence.AdminMarketAssetStatRepository;
import com.company.finance_api.instrument.infrastructure.persistence.InstrumentRepository;
import com.company.finance_api.portfolio.infrastructure.persistence.TransactionRepository;
import com.company.finance_api.watchlist.infrastructure.persistence.WatchlistItemRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Watchlist ve external portfolio holding'lerinden admin market-asset analytics üretir ve persist
 * eder.
 */
@Service
public class AdminMarketAssetAnalyticsService {

  private static final Logger log = LoggerFactory.getLogger(AdminMarketAssetAnalyticsService.class);
  private static final int MAX_PAGE_SIZE = 50;

  private final AdminMarketAssetSnapshotRepository snapshotRepository;
  private final AdminMarketAssetStatRepository statRepository;
  private final TransactionRepository transactionRepository;
  private final PortfolioPositionBuilder portfolioPositionBuilder;
  private final ExternalPortfolioRepository externalPortfolioRepository;
  private final ExternalPositionLotRepository externalPositionLotRepository;
  private final ExternalPortfolioValuationService valuationService;
  private final WatchlistItemRepository watchlistItemRepository;
  private final InstrumentRepository instrumentRepository;
  private final AdminMarketAssetRecomputeWorker recomputeWorker;
  private final AtomicBoolean recomputeRunning = new AtomicBoolean(false);

  public AdminMarketAssetAnalyticsService(
      AdminMarketAssetSnapshotRepository snapshotRepository,
      AdminMarketAssetStatRepository statRepository,
      TransactionRepository transactionRepository,
      PortfolioPositionBuilder portfolioPositionBuilder,
      ExternalPortfolioRepository externalPortfolioRepository,
      ExternalPositionLotRepository externalPositionLotRepository,
      ExternalPortfolioValuationService valuationService,
      WatchlistItemRepository watchlistItemRepository,
      InstrumentRepository instrumentRepository,
      @Lazy AdminMarketAssetRecomputeWorker recomputeWorker) {
    this.snapshotRepository = snapshotRepository;
    this.statRepository = statRepository;
    this.transactionRepository = transactionRepository;
    this.portfolioPositionBuilder = portfolioPositionBuilder;
    this.externalPortfolioRepository = externalPortfolioRepository;
    this.externalPositionLotRepository = externalPositionLotRepository;
    this.valuationService = valuationService;
    this.watchlistItemRepository = watchlistItemRepository;
    this.instrumentRepository = instrumentRepository;
    this.recomputeWorker = recomputeWorker;
  }

  /** Önceden hesaplanmış snapshot ve sıralı instrument tablosunu döner. */
  @Transactional(readOnly = true)
  public AdminMarketAssetDashboardDto dashboard(int page, int size) {
    int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    int safePage = Math.max(page, 0);
    AdminMarketAssetSnapshot snapshot = loadSnapshot();
    Page<AdminMarketAssetStat> stats =
        statRepository.findAllByOrderBySortRankAsc(PageRequest.of(safePage, safeSize));
    AdminMarketAssetStatsPageDto table = toPageDto(stats);
    return new AdminMarketAssetDashboardDto(
        snapshot.getStatus(),
        snapshot.getStartedAt(),
        snapshot.getComputedAt(),
        snapshot.getAvgWatchlistInstrumentsPerUser(),
        snapshot.getAvgInstrumentsPerPortfolio(),
        snapshot.getAvgPortfolioWeightPercent(),
        snapshot.getInstrumentRowCount(),
        snapshot.getErrorMessage(),
        table);
  }

  /** Arka planda yeniden hesaplamayı başlatır; zaten çalışıyorsa reddeder. */
  public AdminMarketAssetRecomputeResponseDto startRecompute() {
    AdminMarketAssetSnapshot snapshot = loadSnapshot();
    if ("RUNNING".equals(snapshot.getStatus()) || !recomputeRunning.compareAndSet(false, true)) {
      return new AdminMarketAssetRecomputeResponseDto(snapshot.getStatus(), false);
    }
    markRunning();
    recomputeWorker.run();
    return new AdminMarketAssetRecomputeResponseDto("RUNNING", true);
  }

  void releaseRecomputeLock() {
    recomputeRunning.set(false);
  }

  /** Snapshot durumunu RUNNING olarak işaretler. */
  @Transactional
  public void markRunning() {
    AdminMarketAssetSnapshot snapshot = loadSnapshot();
    snapshot.setStatus("RUNNING");
    snapshot.setStartedAt(Instant.now());
    snapshot.setComputedAt(null);
    snapshot.setErrorMessage(null);
    snapshotRepository.save(snapshot);
  }

  /** Snapshot'ı FAILED durumuna alır ve hata mesajını kaydeder. */
  @Transactional
  public void markFailed(String message) {
    AdminMarketAssetSnapshot snapshot = loadSnapshot();
    snapshot.setStatus("FAILED");
    snapshot.setErrorMessage(truncate(message, 500));
    snapshotRepository.save(snapshot);
  }

  /** Tüm instrument istatistiklerini yeniden hesaplayıp veritabanına yazar. */
  @Transactional
  public void recomputeAndPersist() {
    Instant started = Instant.now();
    statRepository.deleteAllRows();

    Map<Long, InstrumentAccumulator> byInstrument = new HashMap<>();
    int portfoliosWithPositions = 0;
    int totalPositionWeights = 0;
    BigDecimal weightSum = BigDecimal.ZERO;
    int totalInstrumentsInPortfolios = 0;

    AggregationTotals txTotals = aggregateFromTransactions(byInstrument);
    portfoliosWithPositions += txTotals.portfoliosWithPositions();
    totalInstrumentsInPortfolios += txTotals.instrumentsInPortfolios();
    totalPositionWeights += txTotals.positionWeights();
    weightSum = weightSum.add(txTotals.weightSum());

    AggregationTotals lotTotals = aggregateFromExternalLots(byInstrument);
    portfoliosWithPositions += lotTotals.portfoliosWithPositions();
    totalInstrumentsInPortfolios += lotTotals.instrumentsInPortfolios();
    totalPositionWeights += lotTotals.positionWeights();
    weightSum = weightSum.add(lotTotals.weightSum());

    Double watchlistAvgRaw =
        watchlistItemRepository.averageActiveWatchlistInstrumentsPerRosterUser();
    BigDecimal avgWatchlist = toScaled(watchlistAvgRaw != null ? watchlistAvgRaw : 0d);

    BigDecimal avgInstrumentsPerPortfolio =
        portfoliosWithPositions == 0
            ? BigDecimal.ZERO
            : toScaled((double) totalInstrumentsInPortfolios / portfoliosWithPositions);

    BigDecimal avgWeight =
        totalPositionWeights == 0
            ? BigDecimal.ZERO
            : weightSum.divide(BigDecimal.valueOf(totalPositionWeights), 4, RoundingMode.HALF_UP);

    List<ComputedInstrumentRow> ranked = new ArrayList<>();
    for (Map.Entry<Long, InstrumentAccumulator> entry : byInstrument.entrySet()) {
      InstrumentAccumulator acc = entry.getValue();
      Instrument instrument = instrumentRepository.findById(entry.getKey()).orElse(null);
      if (instrument == null) {
        continue;
      }
      BigDecimal avgInstrumentWeight = average(acc.weightPercents);
      ranked.add(
          new ComputedInstrumentRow(
              instrument.getId(),
              instrument.getSymbol(),
              instrument.getName() != null ? instrument.getName() : instrument.getSymbol(),
              acc.portfolioIds.size(),
              acc.userIds.size(),
              avgInstrumentWeight));
    }
    ranked.sort(Comparator.comparing(ComputedInstrumentRow::avgWeightPercent).reversed());

    List<AdminMarketAssetStat> entities = new ArrayList<>();
    int rank = 1;
    for (ComputedInstrumentRow row : ranked) {
      entities.add(
          new AdminMarketAssetStat(
              row.instrumentId(),
              row.symbol(),
              row.instrumentName(),
              row.portfolioCount(),
              row.userCount(),
              row.avgWeightPercent(),
              rank++));
    }
    if (!entities.isEmpty()) {
      statRepository.saveAll(entities);
    }

    AdminMarketAssetSnapshot snapshot = loadSnapshot();
    snapshot.setStatus("READY");
    snapshot.setComputedAt(Instant.now());
    snapshot.setStartedAt(started);
    snapshot.setAvgWatchlistInstrumentsPerUser(avgWatchlist);
    snapshot.setAvgInstrumentsPerPortfolio(avgInstrumentsPerPortfolio);
    snapshot.setAvgPortfolioWeightPercent(avgWeight);
    snapshot.setInstrumentRowCount(entities.size());
    snapshot.setErrorMessage(null);
    snapshotRepository.save(snapshot);
    log.info(
        "Admin market asset snapshot saved rows={} portfolios={}",
        entities.size(),
        portfoliosWithPositions);
  }

  private AdminMarketAssetSnapshot loadSnapshot() {
    return snapshotRepository
        .findById(AdminMarketAssetSnapshot.SINGLETON_ID)
        .orElseThrow(
            () ->
                new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "snapshot missing"));
  }

  private static AdminMarketAssetStatsPageDto toPageDto(Page<AdminMarketAssetStat> page) {
    List<AdminMarketAssetStatRowDto> content =
        page.getContent().stream()
            .map(
                s ->
                    new AdminMarketAssetStatRowDto(
                        s.getInstrumentId(),
                        s.getSymbol(),
                        s.getInstrumentName(),
                        s.getPortfolioCount(),
                        s.getUserCount(),
                        s.getAvgWeightPercent(),
                        s.getSortRank()))
            .toList();
    return new AdminMarketAssetStatsPageDto(
        content, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
  }

  private static BigDecimal average(List<BigDecimal> values) {
    if (values.isEmpty()) {
      return BigDecimal.ZERO;
    }
    BigDecimal sum = BigDecimal.ZERO;
    for (BigDecimal v : values) {
      sum = sum.add(v);
    }
    return sum.divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
  }

  private static BigDecimal toScaled(double value) {
    return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
  }

  private static String truncate(String message, int max) {
    if (message == null) {
      return null;
    }
    return message.length() <= max ? message : message.substring(0, max);
  }

  private AggregationTotals aggregateFromTransactions(
      Map<Long, InstrumentAccumulator> byInstrument) {
    List<Transaction> transactions = transactionRepository.findAllWithExternalPortfolio();
    if (transactions.isEmpty()) {
      return AggregationTotals.EMPTY;
    }
    Map<Long, List<Transaction>> byPortfolio =
        transactions.stream().collect(Collectors.groupingBy(t -> t.getExternalPortfolio().getId()));

    int portfoliosWithPositions = 0;
    int instrumentsInPortfolios = 0;
    int positionWeights = 0;
    BigDecimal weightSum = BigDecimal.ZERO;

    for (Map.Entry<Long, List<Transaction>> portfolioEntry : byPortfolio.entrySet()) {
      Long portfolioId = portfolioEntry.getKey();
      List<Transaction> portfolioTxs = portfolioEntry.getValue();
      UUID userId = portfolioTxs.get(0).getUser().getId();

      Map<Long, List<Transaction>> byInstrumentTx =
          portfolioTxs.stream().collect(Collectors.groupingBy(t -> t.getInstrument().getId()));

      List<WeightedHolding> holdings = new ArrayList<>();
      for (List<Transaction> instrumentTxs : byInstrumentTx.values()) {
        Instrument instrument = instrumentTxs.get(0).getInstrument();
        Optional<PortfolioPosition> positionOpt =
            portfolioPositionBuilder.build(instrument, instrumentTxs);
        if (positionOpt.isEmpty()) {
          continue;
        }
        PortfolioPosition position = positionOpt.get();
        BigDecimal value = valuationAmount(position);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
          continue;
        }
        holdings.add(new WeightedHolding(instrument.getId(), portfolioId, userId, value));
      }

      AggregationTotals slice = applyPortfolioHoldings(byInstrument, holdings);
      if (slice.portfoliosWithPositions() > 0) {
        portfoliosWithPositions++;
        instrumentsInPortfolios += holdings.size();
        positionWeights += slice.positionWeights();
        weightSum = weightSum.add(slice.weightSum());
      }
    }
    return new AggregationTotals(
        portfoliosWithPositions, instrumentsInPortfolios, positionWeights, weightSum);
  }

  private AggregationTotals aggregateFromExternalLots(
      Map<Long, InstrumentAccumulator> byInstrument) {
    Set<Long> portfoliosWithTransactions =
        transactionRepository.findAllWithExternalPortfolio().stream()
            .map(t -> t.getExternalPortfolio().getId())
            .collect(Collectors.toSet());

    int portfoliosWithPositions = 0;
    int instrumentsInPortfolios = 0;
    int positionWeights = 0;
    BigDecimal weightSum = BigDecimal.ZERO;

    for (Object[] row : externalPortfolioRepository.findAllPortfolioIdAndUserId()) {
      Long portfolioId = ((Number) row[0]).longValue();
      if (portfoliosWithTransactions.contains(portfolioId)) {
        continue;
      }
      if (externalPositionLotRepository
          .findAllByPortfolioIdAndDeletedFalseOrderByAcquiredAtAsc(portfolioId)
          .isEmpty()) {
        continue;
      }
      UUID userId = (UUID) row[1];
      ExternalPortfolioSummaryResponse summary;
      try {
        summary = valuationService.calculateSummary(userId, portfolioId);
      } catch (RuntimeException ex) {
        log.warn(
            "Skipping lot portfolio valuation portfolioId={} userId={}", portfolioId, userId, ex);
        continue;
      }
      if (summary.getPositions() == null || summary.getPositions().isEmpty()) {
        continue;
      }
      BigDecimal totalMarket = summary.getTotalMarketValue();
      if (totalMarket == null || totalMarket.compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      List<WeightedHolding> holdings = new ArrayList<>();
      for (ExternalPositionSummary pos : summary.getPositions()) {
        if (pos.getMarketValue() == null || pos.getMarketValue().compareTo(BigDecimal.ZERO) <= 0) {
          continue;
        }
        holdings.add(
            new WeightedHolding(pos.getInstrumentId(), portfolioId, userId, pos.getMarketValue()));
      }
      AggregationTotals slice = applyPortfolioHoldings(byInstrument, holdings);
      if (slice.portfoliosWithPositions() > 0) {
        portfoliosWithPositions++;
        instrumentsInPortfolios += holdings.size();
        positionWeights += slice.positionWeights();
        weightSum = weightSum.add(slice.weightSum());
      }
    }
    return new AggregationTotals(
        portfoliosWithPositions, instrumentsInPortfolios, positionWeights, weightSum);
  }

  private static BigDecimal valuationAmount(PortfolioPosition position) {
    if (position.hasPrice() && position.currentValue().compareTo(BigDecimal.ZERO) > 0) {
      return position.currentValue();
    }
    return position.totalCost();
  }

  private static AggregationTotals applyPortfolioHoldings(
      Map<Long, InstrumentAccumulator> byInstrument, List<WeightedHolding> holdings) {
    if (holdings.isEmpty()) {
      return AggregationTotals.EMPTY;
    }
    BigDecimal total = BigDecimal.ZERO;
    for (WeightedHolding h : holdings) {
      total = total.add(h.value());
    }
    if (total.compareTo(BigDecimal.ZERO) <= 0) {
      return AggregationTotals.EMPTY;
    }
    int positionWeights = 0;
    BigDecimal weightSum = BigDecimal.ZERO;
    for (WeightedHolding h : holdings) {
      BigDecimal weight =
          h.value().divide(total, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
      weightSum = weightSum.add(weight);
      positionWeights++;
      InstrumentAccumulator acc =
          byInstrument.computeIfAbsent(h.instrumentId(), k -> new InstrumentAccumulator());
      acc.portfolioIds.add(h.portfolioId());
      acc.userIds.add(h.userId());
      acc.weightPercents.add(weight);
    }
    return new AggregationTotals(1, holdings.size(), positionWeights, weightSum);
  }

  private record WeightedHolding(
      Long instrumentId, Long portfolioId, UUID userId, BigDecimal value) {}

  private record AggregationTotals(
      int portfoliosWithPositions,
      int instrumentsInPortfolios,
      int positionWeights,
      BigDecimal weightSum) {
    static final AggregationTotals EMPTY = new AggregationTotals(0, 0, 0, BigDecimal.ZERO);
  }

  private static final class InstrumentAccumulator {
    final Set<Long> portfolioIds = new HashSet<>();
    final Set<UUID> userIds = new HashSet<>();
    final List<BigDecimal> weightPercents = new ArrayList<>();
  }

  private record ComputedInstrumentRow(
      Long instrumentId,
      String symbol,
      String instrumentName,
      int portfolioCount,
      int userCount,
      BigDecimal avgWeightPercent) {}
}
