package com.company.marketdataservice.shared.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Duration;
import java.util.Optional;

/** JSON tabanlı kısa ömürlü response cache contract'ıdır. */
public interface JsonCacheService {

  /** Verilen cache key için payload'ı okuyup hedef tipe deserialize eder. */
  <T> Optional<T> get(String key, TypeReference<T> typeReference);

  /** Değeri JSON olarak serialize edip belirtilen TTL ile yazar. */
  void put(String key, Object value, Duration ttl);

  /** Verilen cache key için mevcut girdiyi siler. */
  void evict(String key);
}
