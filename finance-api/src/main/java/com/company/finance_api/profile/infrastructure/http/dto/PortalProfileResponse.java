package com.company.finance_api.profile.infrastructure.http.dto;

import java.time.Instant;

/** PortalProfileResponse — API transfer nesnesi (DTO/response/request). */
public record PortalProfileResponse(
    String email,
    String username,
    String phone,
    boolean notifySecurityAlerts,
    boolean notifyProductUpdates,
    Instant avatarUpdatedAt,
    String preferredLocale,
    String preferredCurrency,
    boolean totpEnabled) {}
