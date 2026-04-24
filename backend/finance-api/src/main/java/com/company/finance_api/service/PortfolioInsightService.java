package com.company.finance_api.service;

import com.company.finance_api.dto.InsightDto;
import com.company.finance_api.dto.PortfolioValuationAssetDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PortfolioInsightService {

    List<InsightDto> analyze(
            List<PortfolioValuationAssetDto> assets,
            Map<String, BigDecimal> segmentBreakdownPercent
    );
}
