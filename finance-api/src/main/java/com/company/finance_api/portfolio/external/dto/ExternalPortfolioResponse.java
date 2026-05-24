package com.company.finance_api.portfolio.external.dto;

import lombok.Builder;
import lombok.Data;

/** External portfolio API response DTO'su. */
@Data
@Builder
public class ExternalPortfolioResponse {

  private Long id;
  private String name;
  private String baseCurrency;
  private String createdAt;
  private boolean amountsHidden;
}
