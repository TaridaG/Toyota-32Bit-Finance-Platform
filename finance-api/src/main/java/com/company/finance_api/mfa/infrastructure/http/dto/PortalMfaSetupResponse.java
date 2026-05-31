package com.company.finance_api.mfa.infrastructure.http.dto;

/** PortalMfaSetupResponse — API transfer nesnesi (DTO/response/request). */
public record PortalMfaSetupResponse(String secret, String otpAuthUri, String qrCodeBase64) {}
