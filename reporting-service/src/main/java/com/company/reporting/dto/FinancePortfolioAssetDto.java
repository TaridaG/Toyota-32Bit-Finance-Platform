package com.company.reporting.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class FinancePortfolioAssetDto {

    private Long instrumentId;
    private String symbol;
    private BigDecimal quantity;
    private BigDecimal avgBuyPrice;
    private BigDecimal currentPrice;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
}