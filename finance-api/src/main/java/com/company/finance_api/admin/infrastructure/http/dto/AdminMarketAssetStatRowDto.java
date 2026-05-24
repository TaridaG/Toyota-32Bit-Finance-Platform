package com.company.finance_api.admin.infrastructure.http.dto;

import java.math.BigDecimal;

/** Tek instrument için portfolio/kullanıcı sayısı ve ortalama ağırlık. */
public record AdminMarketAssetStatRowDto(
    Long instrumentId,
    String symbol,
    String instrumentName,
    int portfolioCount,
    int userCount,
    BigDecimal avgWeightPercent,
    int rank) {}
