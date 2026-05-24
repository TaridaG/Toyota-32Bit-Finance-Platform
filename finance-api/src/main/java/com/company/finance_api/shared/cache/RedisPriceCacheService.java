package com.company.finance_api.shared.cache;

import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import java.time.Duration;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * {@code cache-redis} profilinde Redis tabanlı instrument fiyat cache implementasyonu (5 dk TTL).
 */
@Service
@Profile("cache-redis")
public class RedisPriceCacheService implements PriceCacheService {

  private static final Duration TTL = Duration.ofMinutes(5);

  private final RedisTemplate<String, InstrumentPrice> redisTemplate;

  public RedisPriceCacheService(RedisTemplate<String, InstrumentPrice> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  private String key(Long instrumentId, PriceType priceType) {
    return "price:" + instrumentId + ":" + priceType.name();
  }

  /**
   * {@inheritDoc} Serializer değişikliğinden kaynaklı uyumsuz girdileri evict ederek boş dönebilir.
   */
  @Override
  public Optional<InstrumentPrice> getLatestPrice(Long instrumentId, PriceType priceType) {
    String cacheKey = key(instrumentId, priceType);
    try {
      return Optional.ofNullable(redisTemplate.opsForValue().get(cacheKey));
    } catch (ClassCastException ex) {
      // Old cache payloads can be deserialized as LinkedHashMap after serializer changes.
      // Evict incompatible entry and allow DB fallback path.
      redisTemplate.delete(cacheKey);
      return Optional.empty();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void putLatestPrice(InstrumentPrice price) {
    redisTemplate
        .opsForValue()
        .set(key(price.getInstrument().getId(), price.getPriceType()), price, TTL);
  }

  /** {@inheritDoc} */
  @Override
  public void evictLatestPrice(Long instrumentId, PriceType priceType) {
    redisTemplate.delete(key(instrumentId, priceType));
  }
}
