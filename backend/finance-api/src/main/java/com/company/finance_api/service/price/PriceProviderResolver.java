package com.company.finance_api.service.price;

import com.company.finance_api.domain.enums.PriceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PriceProviderResolver {

    private final List<PriceProvider> providers;

    public PriceProvider resolve(PriceType priceType) {
        return providers.stream()
                .filter(p -> p.supports() == priceType)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No PriceProvider found for type: " + priceType
                        )
                );
    }
}