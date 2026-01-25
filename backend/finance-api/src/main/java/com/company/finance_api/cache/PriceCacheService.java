package com.company.finance_api.cache;

import com.company.finance_api.domain.InstrumentPrice;
import com.company.finance_api.domain.enums.PriceType;

import java.util.Optional;

public interface PriceCacheService {

    Optional<InstrumentPrice> getLatestPrice(
            Long instrumentId,
            PriceType priceType
    );

    void putLatestPrice(InstrumentPrice price);

    void evictLatestPrice(Long instrumentId, PriceType priceType);
}
