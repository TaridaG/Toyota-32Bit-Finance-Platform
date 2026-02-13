package com.company.marketdataservice.provider;

import com.company.marketdataservice.config.ResilienceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PriceProviderResolver {

    private final Map<String, PriceProvider> providers;
    private final ResilienceProperties resilienceProperties;

    public PriceProvider resolve(String providerName) {

        PriceProvider provider = providers.get(providerName.toLowerCase());

        if (provider == null) {
            throw new IllegalArgumentException("Unknown provider: " + providerName);
        }

        return new ResilientPriceProvider(provider, resilienceProperties);
    }
}

