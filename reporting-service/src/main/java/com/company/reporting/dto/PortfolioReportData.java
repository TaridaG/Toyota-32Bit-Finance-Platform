package com.company.reporting.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PortfolioReportData {

    private List<PortfolioAssetRow> assets;
    private BigDecimal totalValue;
    private BigDecimal totalProfitLoss;
}