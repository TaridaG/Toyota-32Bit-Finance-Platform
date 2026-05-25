package com.company.finance_api.shared.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class HybridJsonCacheServiceTest {

  @Test
  void fallsBackToInMemoryCacheWhenRedisUnavailable() {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);
    HybridJsonCacheService service = new HybridJsonCacheService(new ObjectMapper(), provider);

    service.put("cache:test:item", new SamplePayload("market-overview"), Duration.ofMinutes(1));

    assertThat(service.get("cache:test:item", new TypeReference<SamplePayload>() {}))
        .contains(new SamplePayload("market-overview"));
  }

  @Test
  void evictsExpiredInMemoryEntries() throws Exception {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);
    HybridJsonCacheService service = new HybridJsonCacheService(new ObjectMapper(), provider);

    service.put("cache:test:ttl", new SamplePayload("short-lived"), Duration.ofMillis(25));

    Thread.sleep(60);

    assertThat(service.get("cache:test:ttl", new TypeReference<SamplePayload>() {})).isEmpty();
  }

  @Test
  void fallsBackToInMemoryCacheWhenRedisWriteAndReadFailAtRuntime() {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    when(provider.getIfAvailable()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(valueOps);
    doThrow(new RuntimeException("redis down"))
        .when(valueOps)
        .set(anyString(), anyString(), any(Duration.class));
    when(valueOps.get("cache:test:runtime")).thenThrow(new RuntimeException("redis down"));

    HybridJsonCacheService service = new HybridJsonCacheService(new ObjectMapper(), provider);
    service.put("cache:test:runtime", new SamplePayload("fallback"), Duration.ofMinutes(1));

    assertThat(service.get("cache:test:runtime", new TypeReference<SamplePayload>() {}))
        .contains(new SamplePayload("fallback"));
  }

  @Test
  void evictRemovesLocalFallbackEntryWhenRedisDeleteFails() {
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    when(provider.getIfAvailable()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(valueOps);
    doThrow(new RuntimeException("redis down"))
        .when(valueOps)
        .set(anyString(), anyString(), any(Duration.class));
    doThrow(new RuntimeException("redis down")).when(redis).delete(eq("cache:test:evict"));
    when(valueOps.get("cache:test:evict")).thenThrow(new RuntimeException("redis down"));

    HybridJsonCacheService service = new HybridJsonCacheService(new ObjectMapper(), provider);
    service.put("cache:test:evict", new SamplePayload("fallback"), Duration.ofMinutes(1));
    service.evict("cache:test:evict");

    assertThat(service.get("cache:test:evict", new TypeReference<SamplePayload>() {})).isEmpty();
  }

  private record SamplePayload(String value) {}
}
