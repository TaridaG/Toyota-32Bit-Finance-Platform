package com.company.finance_api.portfolio.valuation;

import java.math.BigDecimal;

/** Enstrüman güncel fiyatını sağlayan port arayüzü. */
public interface InstrumentPriceProvider {

  /** Verilen enstrüman id için güncel fiyatı döner. */
  BigDecimal getCurrentPrice(Long instrumentId);
}
