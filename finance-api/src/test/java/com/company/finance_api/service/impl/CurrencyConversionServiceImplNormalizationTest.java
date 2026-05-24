package com.company.finance_api.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.junit.jupiter.api.Test;

class CurrencyConversionServiceImplNormalizationTest {

  @Test
  void jpyTry_tcmbPer100_isScaledToTryPerOneJpy() {
    BigDecimal n =
        CurrencyConversionServiceImpl.normalizeFxMidForTryHub("JPYTRY", new BigDecimal("28.7"));
    assertEquals(0, new BigDecimal("0.287").compareTo(n));
  }

  @Test
  void jpyTry_alreadyPerOneJpy_unchanged() {
    BigDecimal n =
        CurrencyConversionServiceImpl.normalizeFxMidForTryHub("JPYTRY", new BigDecimal("0.287"));
    assertEquals(0, new BigDecimal("0.287").compareTo(n));
  }

  @Test
  void jpyUsd_jpyPerUsd_invertedToUsdPerJpy() {
    BigDecimal n =
        CurrencyConversionServiceImpl.normalizeFxMidForTryHub("JPYUSD", new BigDecimal("150"));
    assertEquals(
        0, BigDecimal.ONE.divide(new BigDecimal("150"), 12, RoundingMode.HALF_UP).compareTo(n));
  }

  @Test
  void jpyUsd_usdPer100Jpy_rescaledToUsdPerOneJpy() {
    BigDecimal n =
        CurrencyConversionServiceImpl.normalizeFxMidForTryHub("JPYUSD", new BigDecimal("0.66"));
    assertEquals(0, new BigDecimal("0.0066").compareTo(n));
  }

  @Test
  void jpyUsd_reasonableUsdPerJpy_unchanged() {
    BigDecimal n =
        CurrencyConversionServiceImpl.normalizeFxMidForTryHub("JPYUSD", new BigDecimal("0.0066"));
    assertEquals(0, new BigDecimal("0.0066").compareTo(n));
  }
}
