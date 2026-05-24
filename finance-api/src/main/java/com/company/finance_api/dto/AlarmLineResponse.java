package com.company.finance_api.dto;

import com.company.finance_api.domain.enums.AlarmCondition;
import java.math.BigDecimal;

/** AlarmLineResponse — API transfer nesnesi (DTO/response/request). */
public record AlarmLineResponse(AlarmCondition condition, BigDecimal targetPrice) {}
