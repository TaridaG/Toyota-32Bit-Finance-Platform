package com.company.finance_api.dto;

import java.math.BigDecimal;

public record PortfolioValuationAssetDto(
        Long instrumentId,
        BigDecimal currentValue,
        BigDecimal pnl,
        BigDecimal weight,
        boolean hasPrice
) {
}
