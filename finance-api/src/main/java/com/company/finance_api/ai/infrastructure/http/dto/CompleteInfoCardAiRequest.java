package com.company.finance_api.ai.infrastructure.http.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/** AI ile info-card alan tamamlama isteği DTO'su. */
public record CompleteInfoCardAiRequest(
    @NotBlank @Pattern(regexp = "^(tr|en|de)$") String language,
    @NotBlank String title,
    @NotBlank String shortDescription,
    String category,
    String type,
    String difficulty,
    List<String> fieldsToGenerate) {}
