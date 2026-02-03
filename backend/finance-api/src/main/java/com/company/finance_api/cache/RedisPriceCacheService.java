package com.company.finance_api.cache;

import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@Profile("cache-redis")
public class RedisPriceCacheService implements PriceCacheService {

    private static final Duration TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, InstrumentPrice> redisTemplate;

    public RedisPriceCacheService(
            RedisTemplate<String, InstrumentPrice> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    private String key(Long instrumentId, PriceType priceType) {
        return "price:" + instrumentId + ":" + priceType.name();
    }

    @Override
    public Optional<InstrumentPrice> getLatestPrice(
            Long instrumentId,
            PriceType priceType
    ) {
        return Optional.ofNullable(
                redisTemplate.opsForValue().get(
                        key(instrumentId, priceType)
                )
        );
    }

    @Override
    public void putLatestPrice(InstrumentPrice price) {
        redisTemplate.opsForValue().set(
                key(price.getInstrument().getId(), price.getPriceType()),
                price,
                TTL
        );
    }

    @Override
    public void evictLatestPrice(Long instrumentId, PriceType priceType) {
        redisTemplate.delete(
                key(instrumentId, priceType)
        );
    }
}