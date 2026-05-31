package com.company.finance_api.shared.cache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.company.finance_api.pricing.domain.enums.PriceType;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisPriceCacheServiceTest {

  @Test
  void returnsEmptyWhenRedisReadFails() {
    @SuppressWarnings("unchecked")
    RedisTemplate<String, InstrumentPrice> redisTemplate = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, InstrumentPrice> valueOps = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.get("price:7:MARKET")).thenThrow(new RuntimeException("redis down"));

    RedisPriceCacheService service = new RedisPriceCacheService(redisTemplate);

    assertThat(service.getLatestPrice(7L, PriceType.MARKET)).isEmpty();
  }

  @Test
  void swallowsRedisWriteFailures() {
    @SuppressWarnings("unchecked")
    RedisTemplate<String, InstrumentPrice> redisTemplate = mock(RedisTemplate.class);
    @SuppressWarnings("unchecked")
    ValueOperations<String, InstrumentPrice> valueOps = mock(ValueOperations.class);
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    doThrow(new RuntimeException("redis down"))
        .when(valueOps)
        .set(eq("price:7:MARKET"), any(InstrumentPrice.class), any());

    RedisPriceCacheService service = new RedisPriceCacheService(redisTemplate);

    assertThatCode(() -> service.putLatestPrice(price(7L, PriceType.MARKET)))
        .doesNotThrowAnyException();
  }

  @Test
  void swallowsRedisEvictFailures() {
    @SuppressWarnings("unchecked")
    RedisTemplate<String, InstrumentPrice> redisTemplate = mock(RedisTemplate.class);
    doThrow(new RuntimeException("redis down")).when(redisTemplate).delete("price:7:MARKET");

    RedisPriceCacheService service = new RedisPriceCacheService(redisTemplate);

    assertThatCode(() -> service.evictLatestPrice(7L, PriceType.MARKET)).doesNotThrowAnyException();
  }

  private static InstrumentPrice price(Long instrumentId, PriceType priceType) {
    com.company.finance_api.instrument.domain.Instrument instrument = mock(com.company.finance_api.instrument.domain.Instrument.class);
    InstrumentPrice price = mock(InstrumentPrice.class);
    when(instrument.getId()).thenReturn(instrumentId);
    when(price.getInstrument()).thenReturn(instrument);
    when(price.getPriceType()).thenReturn(priceType);
    return price;
  }
}
