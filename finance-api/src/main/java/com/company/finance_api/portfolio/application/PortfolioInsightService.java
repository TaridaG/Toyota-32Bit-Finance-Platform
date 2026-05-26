package com.company.finance_api.portfolio.application;

import com.company.finance_api.dto.InsightDto;
import com.company.finance_api.dto.PortfolioValuationAssetDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** PortfolioInsightService iş mantığını uygular (portfolio insight service). */
public interface PortfolioInsightService {

  List<InsightDto> analyze(
      List<PortfolioValuationAssetDto> assets, Map<String, BigDecimal> segmentBreakdownPercent);
}
