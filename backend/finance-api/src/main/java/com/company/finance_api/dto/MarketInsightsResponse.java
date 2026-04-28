package com.company.finance_api.dto;

import java.util.List;

public record MarketInsightsResponse(
        List<MarketOverviewItemResponse> topGainers,
        List<MarketOverviewItemResponse> topLosers
) {
}
