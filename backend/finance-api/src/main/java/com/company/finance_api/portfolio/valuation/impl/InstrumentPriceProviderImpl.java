package com.company.finance_api.portfolio.valuation.impl;

import com.company.finance_api.cache.PriceCacheService;
import com.company.finance_api.domain.enums.PriceType;
import com.company.finance_api.portfolio.valuation.InstrumentPriceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class InstrumentPriceProviderImpl implements InstrumentPriceProvider {

    private final PriceCacheService priceCacheService;

    @Override
    public BigDecimal getCurrentPrice(Long instrumentId) {
        return priceCacheService.getLatestPrice(instrumentId, PriceType.MARKET)
                .orElseThrow(() -> new RuntimeException("Price not found for instrumentId=" + instrumentId))
                .getPrice();
    }
}

