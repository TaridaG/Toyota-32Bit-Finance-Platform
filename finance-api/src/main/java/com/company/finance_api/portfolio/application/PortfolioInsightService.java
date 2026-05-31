package com.company.finance_api.portfolio.application;

import com.company.finance_api.portfolio.infrastructure.http.dto.InsightDto;
import com.company.finance_api.portfolio.infrastructure.http.dto.PortfolioValuationAssetDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** PortfolioInsightService iş mantığını uygular (portfolio insight service). */
public interface PortfolioInsightService {

  List<InsightDto> analyze(
      List<PortfolioValuationAssetDto> assets, Map<String, BigDecimal> segmentBreakdownPercent);
}
