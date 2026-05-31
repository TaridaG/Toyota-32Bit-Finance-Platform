package com.company.finance_api.portfolio.external.infrastructure.http.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/** External portfolio değerleme özeti ve pozisyon listesini taşıyan response DTO'su. */
@Data
@Builder
public class ExternalPortfolioSummaryResponse {

  private BigDecimal totalCost;
  private BigDecimal totalMarketValue;
  private BigDecimal totalPnL;
  private BigDecimal totalPnLPercentage;

  private List<ExternalPositionSummary> positions;
}
