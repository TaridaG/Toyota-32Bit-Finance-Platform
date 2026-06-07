package com.company.finance_api.pricing.application;

import com.company.finance_api.portfolio.infrastructure.http.dto.AcquisitionFxRatesSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/** CurrencyConversionService iş mantığını uygular (currency conversion service). */
public interface CurrencyConversionService {

  /** Güncel FX mid snapshot ile {@code from} para biriminden {@code to} para birimine fiyat dönüştürür. */
  BigDecimal convert(BigDecimal price, String from, String to);

  /**
   * {@code mds_fx_rate_history} tablosundaki FX mid'leri {@code fxAsOfEndExclusive} (UTC) anına kadar
   * kullanarak dönüştürür.
   */
  BigDecimal convertAt(Instant fxAsOfEndExclusive, BigDecimal price, String from, String to);

  /** Para birimi kodunu normalize eder (TRY hub ve alias kuralları). */
  String normalizeCurrency(String currency);

  /** Enstrüman sembolü için güncel FX oranını döner. */
  Optional<BigDecimal> getRate(String symbol);

  /**
   * Trade audit / preview için TRY-hub (+ USD leg) FX paneli.
   *
   * @param fxAsOfEndExclusive {@code historical} true iken MDS {@code observed_at} üst sınırı; false
   *     iken canlı {@link #convert} snapshot kullanılır (instant yine DTO'da yansıtılır)
   */
  AcquisitionFxRatesSnapshot acquisitionFxHubSnapshot(
      Instant fxAsOfEndExclusive, boolean historical);
}
