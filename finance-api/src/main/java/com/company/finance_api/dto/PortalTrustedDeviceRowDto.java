package com.company.finance_api.dto;

import java.time.Instant;
import java.util.UUID;

/** PortalTrustedDeviceRowDto — API transfer nesnesi (DTO/response/request). */
public record PortalTrustedDeviceRowDto(
    UUID id, Instant createdAt, Instant lastUsedAt, Instant expiresAt, boolean currentDevice) {}
