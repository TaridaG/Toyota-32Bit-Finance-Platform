package com.company.finance_api.portfolio.goal.infrastructure.http.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Kar hedefi oluşturma veya güncelleme isteği DTO'su. */
public class UpsertProfitGoalRequest {

  private Long portfolioId;

  @NotBlank private String profitTargetMode;

  @DecimalMin(value = "0.01", inclusive = true)
  private BigDecimal targetAmount;

  @DecimalMin(value = "0.01", inclusive = true)
  private BigDecimal targetPercent;

  @Size(max = 200)
  private String title;

  @Size(max = 2000)
  private String description;

  public Long getPortfolioId() {
    return portfolioId;
  }

  public void setPortfolioId(Long portfolioId) {
    this.portfolioId = portfolioId;
  }

  public String getProfitTargetMode() {
    return profitTargetMode;
  }

  public void setProfitTargetMode(String profitTargetMode) {
    this.profitTargetMode = profitTargetMode;
  }

  public BigDecimal getTargetAmount() {
    return targetAmount;
  }

  public void setTargetAmount(BigDecimal targetAmount) {
    this.targetAmount = targetAmount;
  }

  public BigDecimal getTargetPercent() {
    return targetPercent;
  }

  public void setTargetPercent(BigDecimal targetPercent) {
    this.targetPercent = targetPercent;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
