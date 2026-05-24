package com.company.finance_api.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** AI ile info-card çeviri isteği DTO'su. */
public record TranslateInfoCardAiRequest(
    @NotBlank @Pattern(regexp = "^(tr|en|de)$") String sourceLanguage,
    @NotBlank @Pattern(regexp = "^(tr|en|de)$") String targetLanguage,
    @NotNull @Valid InfoCardAiSourceContent sourceContent) {}
