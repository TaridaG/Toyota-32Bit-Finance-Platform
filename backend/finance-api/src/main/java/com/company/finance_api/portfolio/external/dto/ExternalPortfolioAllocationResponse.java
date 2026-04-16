package com.company.finance_api.portfolio.external.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ExternalPortfolioAllocationResponse {

    private String symbol;
    private BigDecimal marketValue;
    private BigDecimal percentage;
}

