package com.company.finance_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** PortalMfaConfirmRequest — API transfer nesnesi (DTO/response/request). */
public record PortalMfaConfirmRequest(@NotBlank @Pattern(regexp = "\\d{6}") String code) {}
