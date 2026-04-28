package com.company.finance_api.dto;

import java.math.BigDecimal;

public record MarketOverviewItemResponse(
        String symbol,
        String name,
        BigDecimal price,
        BigDecimal change24h,
        BigDecimal high24h,
        BigDecimal low24h,
        String category,
        Long instrumentId
) {
}
