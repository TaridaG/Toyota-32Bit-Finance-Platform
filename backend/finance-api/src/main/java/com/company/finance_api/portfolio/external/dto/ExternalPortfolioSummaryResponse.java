package com.company.finance_api.portfolio.external.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ExternalPortfolioSummaryResponse {

    private BigDecimal totalCost;
    private BigDecimal totalMarketValue;
    private BigDecimal totalPnL;
    private BigDecimal totalPnLPercentage;

    private List<ExternalPositionSummary> positions;
}

