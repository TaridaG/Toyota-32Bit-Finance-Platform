package com.company.finance_api.shared.cache;

import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import java.util.Optional;

/**
 * Instrument son fiyat cache soyutlaması; Redis veya in-memory implementasyonlar tarafından
 * kullanılır.
 */
public interface PriceCacheService {

  /** Cache'ten en son fiyatı okur. */
  Optional<InstrumentPrice> getLatestPrice(Long instrumentId, PriceType priceType);

  /** En son fiyatı cache'e yazar. */
  void putLatestPrice(InstrumentPrice price);

  /** Belirtilen instrument/priceType için cache girdisini siler. */
  void evictLatestPrice(Long instrumentId, PriceType priceType);
}
