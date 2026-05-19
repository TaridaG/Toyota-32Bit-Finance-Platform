package com.company.finance_api.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CompleteInfoCardAiRequest(
        @NotBlank
        @Pattern(regexp = "^(tr|en|de)$")
        String language,
        @NotBlank String title,
        @NotBlank String shortDescription,
        String category,
        String type,
        String difficulty,
        List<String> fieldsToGenerate
) {
}
