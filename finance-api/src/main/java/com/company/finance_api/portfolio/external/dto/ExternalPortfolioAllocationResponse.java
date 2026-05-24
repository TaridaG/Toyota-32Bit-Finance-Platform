package com.company.finance_api.portfolio.external.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;

/** Portfolio içindeki tek sembolün piyasa değeri ve yüzde payını taşıyan DTO. */
@Data
@Builder
public class ExternalPortfolioAllocationResponse {

  private String symbol;
  private BigDecimal marketValue;
  private BigDecimal percentage;
}
