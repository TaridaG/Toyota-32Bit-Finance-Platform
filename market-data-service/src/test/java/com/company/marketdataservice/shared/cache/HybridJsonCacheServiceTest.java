package com.company.marketdataservice.shared.cache;

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
    @SuppressWarnings("unchecked")
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);
    HybridJsonCacheService service = new HybridJsonCacheService(new ObjectMapper(), provider);

    service.put("mds:hot:test:item", new SamplePayload("prices"), Duration.ofMinutes(1));

    assertThat(service.get("mds:hot:test:item", new TypeReference<SamplePayload>() {}))
        .contains(new SamplePayload("prices"));
  }

  @Test
  void evictsExpiredInMemoryEntries() throws Exception {
    @SuppressWarnings("unchecked")
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);
    HybridJsonCacheService service = new HybridJsonCacheService(new ObjectMapper(), provider);

    service.put("mds:hot:test:ttl", new SamplePayload("short-lived"), Duration.ofMillis(25));

    Thread.sleep(60);

    assertThat(service.get("mds:hot:test:ttl", new TypeReference<SamplePayload>() {})).isEmpty();
  }

  @Test
  void fallsBackToInMemoryCacheWhenRedisWriteAndReadFailAtRuntime() {
    @SuppressWarnings("unchecked")
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    when(provider.getIfAvailable()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(valueOps);
    doThrow(new RuntimeException("redis down"))
        .when(valueOps)
        .set(anyString(), anyString(), any(Duration.class));
    when(valueOps.get("mds:hot:test:runtime")).thenThrow(new RuntimeException("redis down"));

    HybridJsonCacheService service = new HybridJsonCacheService(new ObjectMapper(), provider);
    service.put("mds:hot:test:runtime", new SamplePayload("fallback"), Duration.ofMinutes(1));

    assertThat(service.get("mds:hot:test:runtime", new TypeReference<SamplePayload>() {}))
        .contains(new SamplePayload("fallback"));
  }

  @Test
  void evictRemovesLocalFallbackEntryWhenRedisDeleteFails() {
    @SuppressWarnings("unchecked")
    ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
    StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    when(provider.getIfAvailable()).thenReturn(redis);
    when(redis.opsForValue()).thenReturn(valueOps);
    doThrow(new RuntimeException("redis down"))
        .when(valueOps)
        .set(anyString(), anyString(), any(Duration.class));
    doThrow(new RuntimeException("redis down")).when(redis).delete(eq("mds:hot:test:evict"));
    when(valueOps.get("mds:hot:test:evict")).thenThrow(new RuntimeException("redis down"));

    HybridJsonCacheService service = new HybridJsonCacheService(new ObjectMapper(), provider);
    service.put("mds:hot:test:evict", new SamplePayload("fallback"), Duration.ofMinutes(1));
    service.evict("mds:hot:test:evict");

    assertThat(service.get("mds:hot:test:evict", new TypeReference<SamplePayload>() {})).isEmpty();
  }

  private record SamplePayload(String value) {}
}
