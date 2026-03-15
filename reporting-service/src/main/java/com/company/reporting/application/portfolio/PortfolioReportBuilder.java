package com.company.reporting.application.portfolio;

import com.company.reporting.dto.FinancePortfolioAssetDto;
import com.company.reporting.dto.PortfolioAssetRow;
import com.company.reporting.dto.PortfolioReportData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PortfolioReportBuilder {

    public PortfolioReportData build(List<FinancePortfolioAssetDto> assets) {
        BigDecimal totalValue = assets.stream()
                .map(FinancePortfolioAssetDto::getCurrentValue)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfitLoss = assets.stream()
                .map(FinancePortfolioAssetDto::getProfitLoss)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<PortfolioAssetRow> rows = assets.stream()
                .map(asset -> PortfolioAssetRow.builder()
                        .symbol(asset.getSymbol())
                        .quantity(asset.getQuantity())
                        .avgBuyPrice(asset.getAvgBuyPrice())
                        .currentPrice(asset.getCurrentPrice())
                        .currentValue(asset.getCurrentValue())
                        .profitLoss(asset.getProfitLoss())
                        .allocation(calculateAllocation(asset.getCurrentValue(), totalValue))
                        .build())
                .toList();

        return PortfolioReportData.builder()
                .assets(rows)
                .totalValue(totalValue)
                .totalProfitLoss(totalProfitLoss)
                .build();
    }

    private BigDecimal calculateAllocation(BigDecimal currentValue, BigDecimal totalValue) {
        if (currentValue == null || totalValue == null || totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentValue
                .multiply(BigDecimal.valueOf(100))
                .divide(totalValue, 4, RoundingMode.HALF_UP);
    }
}