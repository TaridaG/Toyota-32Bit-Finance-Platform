package com.company.finance_api.dto;

import java.math.BigDecimal;

public record MarketOverviewItemResponse(
        String symbol,
        String name,
        BigDecimal price,
        BigDecimal change24h,
        BigDecimal change1D,
        BigDecimal change1M,
        BigDecimal change3M,
        BigDecimal change6M,
        BigDecimal change1Y,
        BigDecimal high24h,
        BigDecimal low24h,
        String category,
        Long instrumentId
) {
}
