package com.company.finance_api.portfolio.external.infrastructure.http.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

/** External portfolio'ya manuel pozisyon ekleme isteği DTO'su. */
@Data
public class CreateExternalPositionRequest {

  @NotNull private Long instrumentId;

  @NotNull
  @DecimalMin(value = "0.00000001")
  private BigDecimal quantity;

  @NotNull
  @DecimalMin(value = "0.00000001")
  private BigDecimal unitPrice;

  private BigDecimal feeAmount;
  private String feeCurrency;

  @NotNull private LocalDateTime acquiredAt;

  private String sourceName;
  private String notes;
}
