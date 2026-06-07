package com.company.finance_api.pricing.application;

import com.company.finance_api.instrument.domain.Instrument;
import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.pricing.domain.enums.PriceType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** PriceService iş mantığını uygular (price service). */
public interface PriceService {

  // Son fiyat (dashboard, alarm, notification)
  Optional<InstrumentPrice> getLatestPrice(Instrument instrument, PriceType priceType);

  /** Enstrüman için güncel valuation fiyatını döner (mark-to-market). */
  Optional<InstrumentPrice> getLatestValuationPrice(Instrument instrument);

  /**
   * {@code exclusiveEnd} anından önceki son valuation tick'ini döner (ör. dün kapanış snapshot için
   * bugün UTC başlangıcı).
   */
  Optional<InstrumentPrice> getLatestValuationPriceBefore(
      Instrument instrument, Instant exclusiveEnd);

  // Grafik için zaman serisi
  List<InstrumentPrice> getPriceHistory(
      Instrument instrument, PriceType priceType, Instant start, Instant end);

  // İleride scheduler / kafka burayı kullanacak
  /** Fiyat kaydını persist eder ve cache'i günceller. */
  InstrumentPrice savePrice(InstrumentPrice price);
}
