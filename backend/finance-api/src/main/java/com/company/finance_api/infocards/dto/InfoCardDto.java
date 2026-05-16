package com.company.finance_api.infocards.dto;

import java.time.Instant;
import java.util.List;

public record InfoCardDto(
        String id,
        String title,
        String slug,
        List<String> targetTerms,
        List<String> targetElementIds,
        List<String> targetInstrumentSymbols,
        List<String> pages,
        String category,
        String type,
        String difficulty,
        String status,
        String shortDescription,
        String detailedDescription,
        String howToInterpret,
        String commonMistake,
        String example,
        List<String> relatedTerms,
        Boolean adminOnly,
        Instant createdAt,
        Instant updatedAt
) {
}
