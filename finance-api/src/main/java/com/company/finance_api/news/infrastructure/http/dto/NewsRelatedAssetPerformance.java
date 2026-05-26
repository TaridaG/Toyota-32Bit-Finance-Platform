package com.company.finance_api.news.infrastructure.http.dto;

import java.math.BigDecimal;

/** NewsRelatedAssetPerformance — API transfer nesnesi (DTO/response/request). */
public record NewsRelatedAssetPerformance(
    String symbol,
    String name,
    BigDecimal price,
    BigDecimal change1d,
    BigDecimal change1w,
    BigDecimal change1m,
    BigDecimal change3m,
    BigDecimal change6m,
    BigDecimal change1y) {}
