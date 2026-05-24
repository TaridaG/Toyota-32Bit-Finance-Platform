package com.company.finance_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** PortalMfaDisableRequest — API transfer nesnesi (DTO/response/request). */
public record PortalMfaDisableRequest(
    @NotBlank String password, @NotBlank @Pattern(regexp = "\\d{6}") String code) {}
