package com.company.finance_api.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record InfoCardAiContentResponse(
        String title,
        String shortDescription,
        String detailedDescription,
        String howToInterpret,
        String commonMistake,
        String example,
        List<String> relatedTerms
) {
}
