package com.company.finance_api.dto;

import java.math.BigDecimal;

/** PortfolioValuationAssetDto — API transfer nesnesi (DTO/response/request). */
public record PortfolioValuationAssetDto(
    Long instrumentId,
    BigDecimal currentValue,
    BigDecimal pnl,
    BigDecimal weight,
    boolean hasPrice) {}
