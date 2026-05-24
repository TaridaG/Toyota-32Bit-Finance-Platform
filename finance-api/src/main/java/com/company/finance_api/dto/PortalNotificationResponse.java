package com.company.finance_api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** PortalNotificationResponse — API transfer nesnesi (DTO/response/request). */
public record PortalNotificationResponse(
    Long id,
    String type,
    String title,
    String body,
    String instrumentSymbol,
    String condition,
    BigDecimal threshold,
    BigDecimal price,
    Instant triggeredAt,
    boolean read) {}
