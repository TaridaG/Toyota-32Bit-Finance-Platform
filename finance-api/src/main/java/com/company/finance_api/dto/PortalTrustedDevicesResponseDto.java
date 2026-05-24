package com.company.finance_api.dto;

import java.util.List;

/** PortalTrustedDevicesResponseDto — API transfer nesnesi (DTO/response/request). */
public record PortalTrustedDevicesResponseDto(
    boolean featureEnabled, List<PortalTrustedDeviceRowDto> devices) {}
