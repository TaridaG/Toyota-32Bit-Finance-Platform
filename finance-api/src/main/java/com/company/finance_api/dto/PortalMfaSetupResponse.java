package com.company.finance_api.dto;

/** PortalMfaSetupResponse — API transfer nesnesi (DTO/response/request). */
public record PortalMfaSetupResponse(String secret, String otpAuthUri, String qrCodeBase64) {}
