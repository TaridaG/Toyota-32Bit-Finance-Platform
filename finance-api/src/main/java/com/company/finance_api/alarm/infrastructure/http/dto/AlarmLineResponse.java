package com.company.finance_api.alarm.infrastructure.http.dto;

import com.company.finance_api.alarm.domain.enums.AlarmCondition;
import java.math.BigDecimal;

/** AlarmLineResponse — API transfer nesnesi (DTO/response/request). */
public record AlarmLineResponse(AlarmCondition condition, BigDecimal targetPrice) {}
