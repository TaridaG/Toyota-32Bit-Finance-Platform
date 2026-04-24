package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PortfolioValuationResponse(
        BigDecimal totalValue,
        List<PortfolioValuationAssetDto> assets,
        Map<String, BigDecimal> segmentBreakdownPercent,
        List<InsightDto> insights
) {
}
