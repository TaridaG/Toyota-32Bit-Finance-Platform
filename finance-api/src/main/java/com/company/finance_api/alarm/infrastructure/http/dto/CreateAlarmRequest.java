package com.company.finance_api.alarm.infrastructure.http.dto;

import com.company.finance_api.alarm.domain.enums.AlarmCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;

/** CreateAlarmRequest — API transfer nesnesi (DTO/response/request). */
@Getter
public class CreateAlarmRequest {

  @NotNull private Long instrumentId;

  @NotNull private AlarmCondition condition;

  @NotNull
  @DecimalMin(value = "0.000001", message = "threshold must be positive")
  private BigDecimal threshold;
}
