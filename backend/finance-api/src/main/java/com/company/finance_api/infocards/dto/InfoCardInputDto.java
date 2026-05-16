package com.company.finance_api.infocards.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record InfoCardInputDto(
        String id,
        String slug,
        @NotBlank String title,
        List<String> targetTerms,
        List<String> targetElementIds,
        List<String> targetInstrumentSymbols,
        @NotEmpty List<String> pages,
        @NotBlank String category,
        @NotBlank String type,
        @NotBlank String difficulty,
        @NotBlank String status,
        @NotBlank String shortDescription,
        String detailedDescription,
        String howToInterpret,
        String commonMistake,
        String example,
        List<String> relatedTerms,
        Boolean adminOnly
) {
}
