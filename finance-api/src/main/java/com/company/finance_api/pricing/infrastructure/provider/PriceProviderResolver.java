package com.company.finance_api.pricing.infrastructure.provider;

import com.company.finance_api.pricing.domain.enums.PriceType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** PriceProviderResolver iş mantığını uygular (price provider resolver). */
@Component
@RequiredArgsConstructor
public class PriceProviderResolver {

  private final List<PriceProvider> providers;

  /** PriceType'a göre uygun PriceProvider'ı seçer. */
  public PriceProvider resolve(PriceType priceType) {
    return providers.stream()
        .filter(p -> p.supports() == priceType)
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("No PriceProvider found for type: " + priceType));
  }
}
