package com.company.finance_api.auth.infrastructure.http.dto;

import java.util.List;

/** PortalTrustedDevicesResponseDto — API transfer nesnesi (DTO/response/request). */
public record PortalTrustedDevicesResponseDto(
    boolean featureEnabled, List<PortalTrustedDeviceRowDto> devices) {}
