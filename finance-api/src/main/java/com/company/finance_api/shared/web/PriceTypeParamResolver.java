package com.company.finance_api.shared.web;

import com.company.finance_api.pricing.domain.enums.PriceType;
import java.util.Locale;

/**
 * Query/path {@code priceType} parametresini {@link PriceType} enum'una çevirir; geçersiz/boş
 * değerde {@code MARKET} döner.
 */
public final class PriceTypeParamResolver {

  private PriceTypeParamResolver() {}

  /** Ham string'i {@link PriceType}'a parse eder; hata durumunda {@code MARKET} fallback. */
  public static PriceType resolve(String raw) {
    if (raw == null || raw.isBlank()) {
      return PriceType.MARKET;
    }
    try {
      return PriceType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return PriceType.MARKET;
    }
  }
}
