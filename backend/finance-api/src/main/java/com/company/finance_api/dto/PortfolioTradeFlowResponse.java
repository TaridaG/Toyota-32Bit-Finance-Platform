package com.company.finance_api.dto;

import java.util.List;

public record PortfolioTradeFlowResponse(
        String currency,
        List<PortfolioTradeFlowPointResponse> points
) {}
