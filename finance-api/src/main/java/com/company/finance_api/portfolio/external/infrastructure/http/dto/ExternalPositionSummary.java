package com.company.finance_api.portfolio.external.infrastructure.http.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/** External portfolio'daki tek enstrüman pozisyon özetini taşıyan DTO. */
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
