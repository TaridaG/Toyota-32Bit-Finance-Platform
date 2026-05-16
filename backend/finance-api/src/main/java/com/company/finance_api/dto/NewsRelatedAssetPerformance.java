package com.company.finance_api.dto;

import java.math.BigDecimal;

public record NewsRelatedAssetPerformance(
        String symbol,
        String name,
        BigDecimal price,
        BigDecimal change1d,
        BigDecimal change1w,
        BigDecimal change1m,
        BigDecimal change3m,
        BigDecimal change6m,
        BigDecimal change1y
) {
}
