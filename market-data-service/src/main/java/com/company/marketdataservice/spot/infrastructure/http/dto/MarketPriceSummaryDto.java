package com.company.marketdataservice.spot.infrastructure.http.dto;
import java.math.BigDecimal;

/**
 * `spot fiyat` REST API için HTTP DTO.
 */
public record MarketPriceSummaryDto(
        BigDecimal price,
        double change1D,
        double change1W,
        double change1M,
        double change3M,
        double change6M,
        double change1Y
) {
}
