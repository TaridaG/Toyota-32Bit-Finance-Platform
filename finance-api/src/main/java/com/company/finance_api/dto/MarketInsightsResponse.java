package com.company.finance_api.dto;

import java.util.List;

/** MarketInsightsResponse — API transfer nesnesi (DTO/response/request). */
public record MarketInsightsResponse(
    List<MarketOverviewItemResponse> topGainers, List<MarketOverviewItemResponse> topLosers) {}
