package com.company.finance_api.cache;

import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Profile("cache-in-memory")
public class InMemoryPriceCacheService implements PriceCacheService {

    private final Map<String, InstrumentPrice> cache = new ConcurrentHashMap<>();

    private String key(Long instrumentId, PriceType priceType) {
        return instrumentId + ":" + priceType.name();
    }

    @Override
    public Optional<InstrumentPrice> getLatestPrice(
            Long instrumentId,
            PriceType priceType
    ) {
        return Optional.ofNullable(
                cache.get(key(instrumentId, priceType))
        );
    }

    @Override
    public void putLatestPrice(InstrumentPrice price) {
        cache.put(
                key(price.getInstrument().getId(), price.getPriceType()),
                price
        );
    }

    @Override
    public void evictLatestPrice(Long instrumentId, PriceType priceType) {
        cache.remove(key(instrumentId, priceType));
    }
}
