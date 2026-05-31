package com.company.finance_api.mfa.infrastructure.http.dto;

import java.time.Instant;

/** PortalMfaStatusResponse — API transfer nesnesi (DTO/response/request). */
public record PortalMfaStatusResponse(boolean enabled, Instant enabledAt) {}
