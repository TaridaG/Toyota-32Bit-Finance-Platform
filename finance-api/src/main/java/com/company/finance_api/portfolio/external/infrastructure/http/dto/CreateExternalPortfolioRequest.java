package com.company.finance_api.portfolio.external.infrastructure.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Yeni external portfolio oluşturma isteği DTO'su. */
@Data
public class CreateExternalPortfolioRequest {

  @NotBlank
  @Size(max = 120)
  private String name;

  private String baseCurrency; // optional
}
