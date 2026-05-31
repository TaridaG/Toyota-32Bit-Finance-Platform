package com.company.finance_api.bootstrap.config;

import com.company.finance_api.pricing.domain.InstrumentPrice;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/** {@code cache-redis} profilinde instrument fiyat cache'i için Redis template bean'i. */
@Configuration
@Profile("cache-redis")
public class RedisConfig {

  /** {@link InstrumentPrice} değerlerini JSON olarak saklayan Redis template bean'i. */
  @Bean
  public RedisTemplate<String, InstrumentPrice> priceRedisTemplate(
      RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
    RedisTemplate<String, InstrumentPrice> template = new RedisTemplate<>();

    template.setConnectionFactory(connectionFactory);

    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper.copy()));

    template.setHashKeySerializer(new StringRedisSerializer());
    template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper.copy()));

    return template;
  }
}
