package com.company.finance_api.instrument.domain.enums;

/** Exchange — domain enum sabitleri. */
public enum Exchange {
  /** Binance kripto borsası (market-data ingest kaynağı). */
  BINANCE,
  /** Borsa İstanbul (BIST) hisse senetleri. */
  BIST,
  /**
   * Legacy / seed edilmiş satırlar: Yahoo Finance kaynaklı BIST tarzı listeler (DB {@code YAHOO}
   * olarak tutulabilir). Hibernate'in mevcut {@code instruments.exchange} değerlerini yükleyebilmesi
   * için korunur.
   */
  YAHOO,
  /** Türk yatırım fonları (TEFAS; DB seed: {@code V32}, {@code V33}). */
  TEFAS,
  /** Türkiye Cumhuriyet Merkez Bankası (TCMB) — FX, mevduat ve TL enstrümanları. */
  TCMB,
  /** NASDAQ hisse senetleri. */
  NASDAQ,
  /** Finnhub API kaynaklı ABD hisse listeleri (NASDAQ segment alternatif exchange). */
  FINNHUB
}
