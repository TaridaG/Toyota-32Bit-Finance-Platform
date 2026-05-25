package com.company.finance_api.shared.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** Redis ve in-memory fallback kullanan ortak response cache implementation'ıdır. */
@Service
public class HybridJsonCacheService implements JsonCacheService {

  private static final Logger log = LoggerFactory.getLogger(HybridJsonCacheService.class);

  private final ObjectMapper objectMapper;
  private final ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider;
  private final ConcurrentHashMap<String, LocalCacheEntry> localCache = new ConcurrentHashMap<>();

  public HybridJsonCacheService(
      ObjectMapper objectMapper, ObjectProvider<StringRedisTemplate> stringRedisTemplateProvider) {
    this.objectMapper = objectMapper;
    this.stringRedisTemplateProvider = stringRedisTemplateProvider;
  }

  /** {@inheritDoc} */
  @Override
  public <T> Optional<T> get(String key, TypeReference<T> typeReference) {
    Optional<String> payload = readPayload(key);
    if (payload.isEmpty()) {
      return Optional.empty();
    }
    try {
      return Optional.of(objectMapper.readValue(payload.get(), typeReference));
    } catch (IOException | RuntimeException ex) {
      log.debug("JSON_CACHE_READ_FAIL key={} reason={}", key, ex.toString());
      evict(key);
      return Optional.empty();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void put(String key, Object value, Duration ttl) {
    try {
      String payload = objectMapper.writeValueAsString(value);
      StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
      if (redis != null) {
        try {
          redis.opsForValue().set(key, payload, ttl);
          localCache.remove(key);
          return;
        } catch (RuntimeException ex) {
          log.debug(
              "JSON_CACHE_WRITE_REDIS_FAIL key={} reason={} fallback=local",
              key,
              ex.toString());
        }
      }
      localCache.put(key, new LocalCacheEntry(payload, Instant.now().plus(ttl)));
    } catch (JsonProcessingException ex) {
      log.debug("JSON_CACHE_WRITE_FAIL key={} reason={}", key, ex.toString());
    }
  }

  /** {@inheritDoc} */
  @Override
  public void evict(String key) {
    StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
    if (redis != null) {
      try {
        redis.delete(key);
      } catch (RuntimeException ex) {
        log.debug(
            "JSON_CACHE_EVICT_REDIS_FAIL key={} reason={} fallback=local",
            key,
            ex.toString());
      }
    }
    localCache.remove(key);
  }

  private Optional<String> readPayload(String key) {
    StringRedisTemplate redis = stringRedisTemplateProvider.getIfAvailable();
    if (redis != null) {
      try {
        Optional<String> payload =
            Optional.ofNullable(redis.opsForValue().get(key)).filter(StringUtils::hasText);
        // When Redis is reachable again, treat it as source of truth and discard any stale
        // emergency local fallback entry.
        localCache.remove(key);
        return payload;
      } catch (RuntimeException ex) {
        log.debug(
            "JSON_CACHE_READ_REDIS_FAIL key={} reason={} fallback=local",
            key,
            ex.toString());
      }
    }

    LocalCacheEntry cached = localCache.get(key);
    if (cached == null) {
      return Optional.empty();
    }
    if (cached.expiresAt().isBefore(Instant.now())) {
      localCache.remove(key);
      return Optional.empty();
    }
    return Optional.of(cached.payload());
  }

  private record LocalCacheEntry(String payload, Instant expiresAt) {}
}
