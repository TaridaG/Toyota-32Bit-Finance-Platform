package com.company.finance_api.shared.cache;

import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** {@code cache-in-memory} profilinde JVM içi instrument fiyat cache implementasyonu. */
@Service
@Profile("cache-in-memory")
public class InMemoryPriceCacheService implements PriceCacheService {

  private final Map<String, InstrumentPrice> cache = new ConcurrentHashMap<>();

  private String key(Long instrumentId, PriceType priceType) {
    return instrumentId + ":" + priceType.name();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<InstrumentPrice> getLatestPrice(Long instrumentId, PriceType priceType) {
    return Optional.ofNullable(cache.get(key(instrumentId, priceType)));
  }

  /** {@inheritDoc} */
  @Override
  public void putLatestPrice(InstrumentPrice price) {
    cache.put(key(price.getInstrument().getId(), price.getPriceType()), price);
  }

  /** {@inheritDoc} */
  @Override
  public void evictLatestPrice(Long instrumentId, PriceType priceType) {
    cache.remove(key(instrumentId, priceType));
  }
}
