package com.company.marketdataservice.dto;

import java.math.BigDecimal;

public record MarketPriceSummaryDto(
        BigDecimal price,
        double change1D,
        double change1M,
        double change3M,
        double change6M,
        double change1Y
) {
}
