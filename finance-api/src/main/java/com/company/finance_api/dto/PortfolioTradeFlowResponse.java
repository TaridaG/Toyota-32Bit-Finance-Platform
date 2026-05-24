package com.company.finance_api.dto;

import java.util.List;

/** PortfolioTradeFlowResponse — API transfer nesnesi (DTO/response/request). */
public record PortfolioTradeFlowResponse(
    String currency, List<PortfolioTradeFlowPointResponse> points) {}
