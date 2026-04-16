package com.company.finance_api.portfolio.external.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExternalPositionSummary {

    private Long instrumentId;
    private String symbol;

    private BigDecimal quantity;
    private BigDecimal avgCost;

    private BigDecimal currentPrice;
    private BigDecimal marketValue;

    private BigDecimal pnl;
    private BigDecimal pnlPercentage;
}

