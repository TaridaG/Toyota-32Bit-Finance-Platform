package com.company.marketdataservice.rates.application;

import com.company.marketdataservice.rates.infrastructure.http.dto.TlDepositIndexHistoryResponseDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.TlDepositIndexLatestDto;
import com.company.marketdataservice.rates.infrastructure.http.dto.TlDepositIndexPointDto;
import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositDailyIndexEntity;
import com.company.marketdataservice.rates.infrastructure.persistence.TlDepositDailyIndexRepository;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TlDepositIndexService {

  private static final ZoneId TR = ZoneId.of("Europe/Istanbul");

  private final TlDepositDailyIndexRepository tlDepositDailyIndexRepository;
  private final TlDepositWeeklySyncService tlDepositWeeklySyncService;
  private final TlDepositDailyIndexSyncService tlDepositDailyIndexSyncService;

  public TlDepositIndexService(
      TlDepositDailyIndexRepository tlDepositDailyIndexRepository,
      TlDepositWeeklySyncService tlDepositWeeklySyncService,
      TlDepositDailyIndexSyncService tlDepositDailyIndexSyncService) {
    this.tlDepositDailyIndexRepository = tlDepositDailyIndexRepository;
    this.tlDepositWeeklySyncService = tlDepositWeeklySyncService;
    this.tlDepositDailyIndexSyncService = tlDepositDailyIndexSyncService;
  }

  public TlDepositIndexLatestDto loadLatest(String maturityParam, LocalDate asOfParam) {
    String maturity = resolveMaturity(maturityParam);
    ensureIndexReady(maturity);
    LocalDate asOf = asOfParam != null ? asOfParam : LocalDate.now(TR);
    TlDepositIndexLatestDto dto = new TlDepositIndexLatestDto();
    dto.setMaturityCode(maturity);
    Optional<TlDepositDailyIndexEntity> row =
        tlDepositDailyIndexRepository.findTopByMaturityCodeAndDayLessThanEqualOrderByDayDesc(maturity, asOf);
    if (row.isEmpty()) {
      return dto;
    }
    TlDepositDailyIndexEntity found = row.get();
    dto.setAsOfDate(found.getDay());
    dto.setIndexValue(found.getIndexValue().setScale(10, RoundingMode.HALF_UP));
    dto.setAnnualRatePercent(found.getAnnualRatePercent().setScale(4, RoundingMode.HALF_UP));
    dto.setSourceObservationDate(found.getSourceObservationDate());
    dto.setSourceProvider(found.getSourceProvider());
    return dto;
  }

  public TlDepositIndexHistoryResponseDto loadHistory(
      String maturityParam, LocalDate fromInclusive, LocalDate toInclusive) {
    String maturity = resolveMaturity(maturityParam);
    ensureIndexReady(maturity);
    LocalDate end = toInclusive != null ? toInclusive : LocalDate.now(TR);
    LocalDate start = fromInclusive != null ? fromInclusive : end.minusYears(5);
    if (start.isAfter(end)) {
      throw new IllegalArgumentException("from must be on or before to");
    }
    List<TlDepositDailyIndexEntity> rows =
        tlDepositDailyIndexRepository.findByMaturityCodeAndDayBetweenOrderByDayAsc(maturity, start, end);
    TlDepositIndexHistoryResponseDto dto = new TlDepositIndexHistoryResponseDto();
    dto.setMaturityCode(maturity);
    dto.setPoints(
        rows.stream()
            .map(
                row ->
                    new TlDepositIndexPointDto(
                        row.getDay(),
                        row.getIndexValue().setScale(10, RoundingMode.HALF_UP),
                        row.getAnnualRatePercent().setScale(4, RoundingMode.HALF_UP),
                        row.getSourceObservationDate()))
            .toList());
    if (!rows.isEmpty()) {
      dto.setSourceProvider(rows.getLast().getSourceProvider());
    }
    return dto;
  }

  private void ensureIndexReady(String maturity) {
    if (tlDepositDailyIndexRepository.countByMaturityCode(maturity) > 0) {
      return;
    }
    Set<String> maturities = tlDepositWeeklySyncService.configuredMaturitySuffixes();
    tlDepositWeeklySyncService.requestInitialSyncIfEmptyAsync();
    tlDepositDailyIndexSyncService.rebuildForMaturities(maturities);
    if (tlDepositDailyIndexRepository.countByMaturityCode(maturity) == 0) {
      tlDepositDailyIndexSyncService.requestInitialBuildIfEmptyAsync(maturities);
    }
  }

  private String resolveMaturity(String raw) {
    Set<String> allowed = tlDepositWeeklySyncService.configuredMaturitySuffixes();
    if (!StringUtils.hasText(raw)) {
      String preferred = tlDepositWeeklySyncService.displayMaturityCode();
      if (allowed.isEmpty() || allowed.contains(preferred)) {
        return preferred;
      }
      return allowed.iterator().next();
    }
    String m = raw.trim().toUpperCase(Locale.ROOT);
    if (allowed.isEmpty() || !allowed.contains(m)) {
      throw new IllegalArgumentException("Unsupported maturity (allowed: " + allowed + ")");
    }
    return m;
  }
}
