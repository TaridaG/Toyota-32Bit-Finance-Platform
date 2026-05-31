package com.company.marketdataservice.shared.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import java.time.Duration;
import java.util.function.Supplier;

/** {@link JsonCacheService} get-or-load yardımcıları. */
public final class JsonCacheSupport {

  private JsonCacheSupport() {}

  public static <T> T getOrLoad(
      JsonCacheService cache,
      String key,
      Duration ttl,
      TypeReference<T> typeReference,
      Supplier<T> loader) {
    return cache.get(key, typeReference).orElseGet(() -> {
      T value = loader.get();
      cache.put(key, value, ttl);
      return value;
    });
  }
}
