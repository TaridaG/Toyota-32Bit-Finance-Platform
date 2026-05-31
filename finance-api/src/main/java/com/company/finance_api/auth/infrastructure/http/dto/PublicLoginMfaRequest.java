package com.company.finance_api.auth.infrastructure.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

/** PublicLoginMfaRequest — API transfer nesnesi (DTO/response/request). */
public record PublicLoginMfaRequest(
    @NotNull UUID challengeId,
    @NotBlank @Pattern(regexp = "\\d{6}") String code,
    boolean trustDevice) {}
